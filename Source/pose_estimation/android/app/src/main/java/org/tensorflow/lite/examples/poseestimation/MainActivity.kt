/* Copyright 2021 The TensorFlow Authors. All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
==============================================================================
*/

package org.tensorflow.lite.examples.poseestimation

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Process
import android.view.SurfaceView
import android.view.View
import android.view.WindowManager
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.tensorflow.lite.examples.poseestimation.camera.CameraSource
import org.tensorflow.lite.examples.poseestimation.data.Device
import org.tensorflow.lite.examples.poseestimation.ml.ModelType
import org.tensorflow.lite.examples.poseestimation.ml.MoveNet
//import org.tensorflow.lite.examples.poseestimation.ml.PoseNet

class MainActivity : AppCompatActivity() {
    companion object {
        private const val FRAGMENT_DIALOG = "dialog"
    }

    /** A [SurfaceView] for camera preview.   */
    private lateinit var surfaceView: SurfaceView // 카메라를 열고 프리뷰(카메라 촬영화면상태) 을 보기 위함

    /** Default pose estimation model is 1 (MoveNet Thunder)
     * 0 == MoveNet Lightning model
     * 1 == MoveNet Thunder model
     * 2 == PoseNet model
     **/
    private var modelPos = 1

    /** Default device is CPU */
    private var device = Device.CPU
    //---------------각 필요한 컴포넌트들을 mainActivity에서 이용하기 위한 변수들
    private var cameraSource: CameraSource? = null
    //---------------

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()// 권한을 요청
        ) { isGranted: Boolean ->
            if (isGranted) {
                // Permission is granted. Continue the action or workflow in your
                // app.
                openCamera() // 카메라 실행
            } else {
                // Explain to the user that the feature is unavailable because the
                // features requires a permission that the user has denied. At the
                // same time, respect the user's decision. Don't link to system
                // settings in an effort to convince the user to change their
                // decision.
                ErrorDialog.newInstance(getString(R.string.tfe_pe_request_permission)) // 에러 메세지를 출력합니다.
                    .show(supportFragmentManager, FRAGMENT_DIALOG)
            }
        }

    //-------------------------------------------스위치에 리스너 추가
    //------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {// 코틀린에서 프로그램 시작시 시작 함수
        super.onCreate(savedInstanceState) //프로그램 시작점
        setContentView(R.layout.activity_main)//activity main 에 따라 화면 설정
        /*
        R은 프로젝트 빌드 시 자동으로 생성되는 클래스
        모든 xml의 리소스 파일 및 id가 들어있다.
         */
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)// 스크린이 계속 켜질 수 있도록
        /*
        화면속 요소들을 가져옴
        나중에 이것으로 화면조작에 따른 어플실행이 가능
        html과 같음
         */
        surfaceView = findViewById(R.id.surfaceView)
        /*
        스피너 위젯을 실행합니다.
        (목록중에 하나 고르는거)
         */
        if (!isCameraPermissionGranted()) {//카메라 접속 허가 없다
            requestPermission()//권한받기
        }
    }

    override fun onStart() { // 생명 주기에 대한 코드 : 시작할 때
        super.onStart()
        openCamera() //카메라 오픈 함수 밑에 있음
    }

    override fun onResume() { // 생명 주기에 대한 코드
        cameraSource?.resume()
        super.onResume()
    }

    override fun onPause() { // 생명 주기에 대한 코드
        cameraSource?.close()
        cameraSource = null
        super.onPause()
    }

    // 카메라 허가가 나있는지에 대한 확인
    private fun isCameraPermissionGranted(): Boolean {
        return checkPermission( // 권한 확인
            Manifest.permission.CAMERA, // 카메라 권한
            Process.myPid(), //프로세스 직접관리 왠지는 모르겠음 공백으로도 함수실행은 가능할 듯
            Process.myUid()
        ) == PackageManager.PERMISSION_GRANTED // 권한이 있다면 1 없다면 0 리턴
    }

    // open camera
    private fun openCamera() {
        if (isCameraPermissionGranted()) { // 카메라가 열려있다면
            if (cameraSource == null) { //org.tensorflow.lite.examples.poseestimation.camera.CameraSource 이 정상상태라면?
                cameraSource = CameraSource(surfaceView).apply {
                    prepareCamera()//카메라 준비
                }//---------- 생명주기에 스레드 실행 추가
                /*
                Dispatchers.Main - 이 디스패처를 사용하여 기본 Android 스레드에서 코루틴을 실행합니다. 이 디스패처는 UI와 상호작용하고 빠른 작업을 실행하기 위해서만 사용해야 합니다. 예를 들어 suspend 함수를 호출하고 Android UI 프레임워크 작업을 실행하며 LiveData 객체를 업데이트합니다.
                Dispatchers.IO - 이 디스패처는 기본 스레드 외부에서 디스크 또는 네트워크 I/O를 실행하도록 최적화되어 있습니다. 예를 들어 회의실 구성요소를 사용하고 파일에서 읽거나 파일에 쓰며 네트워크 작업을 실행합니다.
                Dispatchers.Default - 이 디스패처는 CPU를 많이 사용하는 작업을 기본 스레드 외부에서 실행하도록 최적화되어 있습니다. 예를 들어 목록을 정렬하고 JSON을 파싱합니다.
                 */
                lifecycleScope.launch(Dispatchers.Main) {
                    cameraSource?.initCamera() // 카메라 시작 이곳에 화면 내용 변경 관련 코드가 있을것으로 추정됨
                }
                //----------
            }
            createPoseEstimator() // 모델 가동
        }
    }
    // change model when app is running
    // 모델을 변화시키고 그에따른 모델 재가동
    private fun createPoseEstimator() { // 머신러닝 모델가동
        val poseDetector = when (modelPos) {
            0 -> {
                MoveNet.create(this, device)
            }
            else -> {
                // (1)
                MoveNet.create(this, device, ModelType.Thunder)
            }
//            else -> {
//                PoseNet.create(this, device)
//            }
        }
        cameraSource?.setDetector(poseDetector) // 카메라 영상처리 함수 실행

    }

    private fun requestPermission() { // 카메라 권한을 얻는 함수
        when (PackageManager.PERMISSION_GRANTED) { // 권한을 받았을 때
            ContextCompat.checkSelfPermission( // 카메라 권한이 있나요?
                this,
                Manifest.permission.CAMERA// 카메라 권한
            ) -> {
                // You can use the API that requires the permission.
                openCamera() // 카메라 실행
            }
            else -> {// 권한 받아오기
                // You can directly ask for the permission.
                // The registered ActivityResultCallback gets the result of this request.
                requestPermissionLauncher.launch( // 카메라 권한을 받아오자
                    Manifest.permission.CAMERA// 카메라 권한
                )
            }
        }
    }

    /**
     * Shows an error message dialog.
     */
    //에러 메세지를 담당하는 클래스
    class ErrorDialog : DialogFragment() {
        override fun onCreateDialog(savedInstanceState: Bundle?): Dialog =
            AlertDialog.Builder(activity)
                .setMessage(requireArguments().getString(ARG_MESSAGE))
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    // do nothing
                }
                .create()

        companion object {

            @JvmStatic
            private val ARG_MESSAGE = "message"

            @JvmStatic
            fun newInstance(message: String): ErrorDialog = ErrorDialog().apply {
                arguments = Bundle().apply { putString(ARG_MESSAGE, message) }
            }
        }
    }
}
