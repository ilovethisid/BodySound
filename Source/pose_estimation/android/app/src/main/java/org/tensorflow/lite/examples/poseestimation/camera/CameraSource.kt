package org.tensorflow.lite.examples.poseestimation.camera

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.media.ImageReader
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.Surface
import android.view.SurfaceView
import kotlinx.coroutines.suspendCancellableCoroutine
import org.tensorflow.lite.examples.poseestimation.VisualizationUtils
import org.tensorflow.lite.examples.poseestimation.YuvToRgbConverter
import org.tensorflow.lite.examples.poseestimation.data.Person
//import org.tensorflow.lite.examples.poseestimation.ml.PoseClassifier
import org.tensorflow.lite.examples.poseestimation.ml.PoseDetector
import java.util.*
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CameraSource(
    private val surfaceView: SurfaceView,
    /*
    ? null이 가능한 변수
    이후에 null을 입력할 수 있다.
     */
) {
    companion object {
        private const val PREVIEW_WIDTH = 640
        private const val PREVIEW_HEIGHT = 480

        /** Threshold for confidence score. */
        private const val MIN_CONFIDENCE = .2f
        private const val TAG = "Camera Source"
    }
    private val lock = Any()
    // Any는 모든 타입의 Root version이다 모든 변수를 함유 가능
    private var detector: PoseDetector? = null // org.tensorflow.lite.examples.poseestimation.ml
    private var yuvConverter: YuvToRgbConverter = YuvToRgbConverter(surfaceView.context) // org.tensorflow.lite.examples.poseestimation
    private lateinit var imageBitmap: Bitmap //
    /*
    why late init?
    the bitmap can be later initialized by initcamera()
     */
    /** Frame count that have been processed so far in an one second interval to calculate FPS. */

    /** Detects, characterizes, and connects to a CameraDevice (used for all camera operations) */

    // 카메라의 정보를 가져와서 cameraId 와 imageDimension 에 값을 할당하고, 카메라를 열어야 하기 때문에
    // CameraManager 객체를 가져온다
    // cameramanager 를 사용 하는 순간 lazy 절이 입력된다.
    private val cameraManager: CameraManager by lazy {
        val context = surfaceView.context
        context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    }
    /** Readers used as buffers for camera still shots */
    /** surfaceview 에서 랜더링되는 imagedata에 접근 가능*/
    private var imageReader: ImageReader? = null

    /** The [CameraDevice] that will be opened in this fragment */
    /** 카메라 사용 class*/
    private var camera: CameraDevice? = null

    /** Internal reference to the ongoing [CameraCaptureSession] configured with our parameters */
    /**CameraCaptureSession : 매우 중요한 API로, 프로그램에서 사진을 미리보고 사진을 찍어야 할 경우이 클래스의 인스턴스를 통해 세션이 생성됩니다
     * 미리보기를 제어하는 메소드는 setRepeatingRequest (), 사진을 제어하는 메소드는 capture ()입니다.*/
    private var session: CameraCaptureSession? = null

    /** [HandlerThread] where all buffer reading operations run */

    private var imageReaderThread: HandlerThread? = null

    /** [Handler] corresponding to [imageReaderThread] */
    /**이벤트를 처리하는 시스템 [imageReaderThread]를 위한 */
    private var imageReaderHandler: Handler? = null

    private var cameraId: String = ""

    suspend fun initCamera() {
        camera = openCamera(cameraManager, cameraId)
        /**openCamera()이용 인자는 카메라 아이디, 콜백, 핸들러를 갖는다.
         * 이 함수는 클래스 내부 자체함수 -> openCamera로 이동*/
        imageReader =
            ImageReader.newInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, ImageFormat.YUV_420_888, 3)
        /**set the image's width , height , format &num of image that can be access in one time*/


        /** 리더에 리스너 추가, 새 이미지를 사용할 수 있으면 리스너 작동 **/
        /**reader == this */
        imageReader?.setOnImageAvailableListener({ reader ->
            val image = reader.acquireLatestImage()
            if (image != null) {
                /**비트맵 초기화*/
                if (!::imageBitmap.isInitialized) {
                    imageBitmap =
                        Bitmap.createBitmap(
                            PREVIEW_WIDTH,
                            PREVIEW_HEIGHT,
                            Bitmap.Config.ARGB_8888
                        )
                }
                yuvConverter.yuvToRgb(image, imageBitmap) // org.tensorflow.lite.examples.poseestimation
                // Create rotated version for portrait display
                /** 이미지를 90도 회전시켜 전달한다. */
                val rotateMatrix = Matrix()
                rotateMatrix.postRotate(90.0f)

                val rotatedBitmap = Bitmap.createBitmap(
                    imageBitmap, 0, 0, PREVIEW_WIDTH, PREVIEW_HEIGHT,
                    rotateMatrix, false
                )
                /**이미지 처리*/
                processImage(rotatedBitmap)

                image.close()
                /**이미지를 close함 -> 접근권한 +1*/
            }
        }, imageReaderHandler)

        /**연속적인 미리보기 화면 제공**/
        imageReader?.surface?.let { surface ->
            // surface가 null이 아닐 때 실행됩니다.
            session = createSession(listOf(surface))

            /** 리퀘스트 빌더 구현 **/
            val cameraRequest = camera?.createCaptureRequest(
                CameraDevice.TEMPLATE_PREVIEW
            )?.apply {
                addTarget(surface)
            }

            /** 끊임없이 반복 캡쳐 **/
            cameraRequest?.build()?.let {
                session?.setRepeatingRequest(it, null, null)
            }
        }
    }

    private suspend fun createSession(targets: List<Surface>): CameraCaptureSession =
        suspendCancellableCoroutine { cont -> /** 실행 중지가 가능한 코루틴 **/
            camera?.createCaptureSession(targets, object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(captureSession: CameraCaptureSession) =
                    cont.resume(captureSession)

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cont.resumeWithException(Exception("Session error"))
                }
            }, null)
        }

    @SuppressLint("MissingPermission")
    private suspend fun openCamera(manager: CameraManager, cameraId: String): CameraDevice =
        suspendCancellableCoroutine { cont ->/** 실행 중지가 가능한 코루틴 **/
            manager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) = cont.resume(camera)

                override fun onDisconnected(camera: CameraDevice) {
                    camera.close()
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    if (cont.isActive) cont.resumeWithException(Exception("Camera error"))
                }
            }, imageReaderHandler)
        }

    fun prepareCamera() {
        for (cameraId in cameraManager.cameraIdList) {
            /**현재 장치와 연결된모든 카메라에 대하여*/
            /**각 장치의 후면카메라만을 카메라로 인정한다.*/
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            // We don't use a front facing camera in this sample.
            val cameraDirection = characteristics.get(CameraCharacteristics.LENS_FACING)
            if (cameraDirection != null &&
                cameraDirection == CameraCharacteristics.LENS_FACING_FRONT
            ) {
                continue
            }
            this.cameraId = cameraId
        }
    }

    /**main activity에서 사용한 세팅 함수
     * 모델 직접 설정
     * **/
    fun setDetector(detector: PoseDetector) {
        synchronized(lock) {
            if (this.detector != null) {
                this.detector?.close()
                this.detector = null
            }
            this.detector = detector
        }
    }

    /**main activity에서 사용한 세팅 함수
     * 분류기 직접 설정
     * **/

    /**resume 함수**/
    /**
    fun resume() {
    imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
    imageReaderHandler = Handler(imageReaderThread!!.looper)
    fpsTimer = Timer()
    fpsTimer?.scheduleAtFixedRate(
    object : TimerTask() {
    override fun run() {
    framesPerSecond = frameProcessedInOneSecondInterval
    frameProcessedInOneSecondInterval = 0
    }
    },
    0,
    1000
    )
    }
     **/
    fun resume() {
        imageReaderThread = HandlerThread("imageReaderThread").apply { start() }
        imageReaderHandler = Handler(imageReaderThread!!.looper)
    }
    /**종료 함수**/
    fun close() {
        session?.close()
        session = null
        camera?.close()
        camera = null
        imageReader?.close()
        imageReader = null
        stopImageReaderThread()
        detector?.close()
        detector = null
        /**
        fpsTimer?.cancel()
        fpsTimer = null
         **/
    }

    // process image
    private fun processImage(bitmap: Bitmap) {
        var person: Person? = null
        var classificationResult: List<Pair<String, Float>>? = null
        synchronized(lock) {/**동기화된 작업 실행**/
            detector?.getLeftWristRatio(bitmap)?.let {
                /**it 는 함수를 통해 리턴된 person 값 입니다.**/
                person = it
                /**포즈 classifier 작동중 일 때**/
            }
        }
        /**1초마다 fps를 mainactivity로 보냄**/
        /**
        frameProcessedInOneSecondInterval++
        if (frameProcessedInOneSecondInterval == 1) {
        // send fps to view
        listener?.onFPSListener(framesPerSecond)
        }
         **/
        /**사람의 뼈대를 그리다**/

        person?.let {
            visualize(it, bitmap)
        }
    }


    /** 시각화 : 사람의 뼈대를 그리다.**/
    private fun visualize(person: Person, bitmap: Bitmap) {
        var outputBitmap = bitmap

        if (person.score > MIN_CONFIDENCE) {
            outputBitmap = VisualizationUtils.drawBodyKeypoints(bitmap, person)
        }

        val holder = surfaceView.holder
        val surfaceCanvas = holder.lockCanvas()
        surfaceCanvas?.let { canvas ->
            val screenWidth: Int
            val screenHeight: Int
            val left: Int
            val top: Int

            if (canvas.height > canvas.width) {
                val ratio = outputBitmap.height.toFloat() / outputBitmap.width
                screenWidth = canvas.width
                left = 0
                screenHeight = (canvas.width * ratio).toInt()
                top = (canvas.height - screenHeight) / 2
            } else {
                val ratio = outputBitmap.width.toFloat() / outputBitmap.height
                screenHeight = canvas.height
                top = 0
                screenWidth = (canvas.height * ratio).toInt()
                left = (canvas.width - screenWidth) / 2
            }
            val right: Int = left + screenWidth
            val bottom: Int = top + screenHeight

            canvas.drawBitmap(
                outputBitmap, Rect(0, 0, outputBitmap.width, outputBitmap.height),
                Rect(left, top, right, bottom), null
            )
            surfaceView.holder.unlockCanvasAndPost(canvas)
        }
    }

    private fun stopImageReaderThread() {
        imageReaderThread?.quitSafely()
        try {
            imageReaderThread?.join()
            imageReaderThread = null
            imageReaderHandler = null
        } catch (e: InterruptedException) {
            Log.d(TAG, e.message.toString())
        }
    }
}
