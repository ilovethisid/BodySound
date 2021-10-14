# BodySound

### 1. 배경
![image](https://user-images.githubusercontent.com/29995264/135447416-fc303a7e-106e-4bf4-a267-fae3d9c632c9.png)

최근에는 자기 자신을 표현하고자 하는 사람들이 늘고 있다. 사람들은 Facebook이나 Instagram등을 이용해 자신의 일상을 올려 친구들과 공유하고 있다. 이와 비슷하지만, 다른 형태로 Youtube, TicTok과 같은 동영상 스트리밍 사이트를 이용하여 자신의 일상, 취미등을 남들과 교류하고 소통하기도 한다. 이런 흐름속에서 우리가 어떻게 사람들에게 편의를 줄 수 있을지 생각해보았다. 

우리는 자기 자신을 표현할 수 있는 수단으로 ‘음악’을 생각하게 되었고, 언제, 어디서나 음악을 표현할 수 있는 서비스를 만들자는 생각에 다다르게 되었다. 음악은 어렵다. 음악을 연주하려면 악기가 있어야 되고, 악기를 배우는데도 시간이 오래 걸린다. 바이올린 같은 악기는 한 대에 50만원, 100만원을 넘기도 하며 피아노 같은 악기는 배우는 데만 수년이 걸리기도 한다. 

우리의 앱은 스마트폰과 몸만 있으면 악기를 연주할 수 있으며, 누구나 쉽게 연주할 수 있다. 우리의 앱은 누구나 쉽고 빠르게 연주할 수 있는 악기를 목표로 삼는다. 시공간에 제약이 없으며, 준비물이 없기 때문에 돈을 들이지 않고도, 많은 시간을 쓰지 않고도 누구나 연주할 수 있게 만드는 것이 우리의 목표이다. 음악을 만드는 것이 특별한 도구나 수단이 필요 없다는 것을 알리고 많은 사람들이 쉽게 접할 수 있게 하고 싶다. 

당장 나에게 악기가 존재하지 않더라도, 연주를 할 수 있도록.

### 2. 목표
- 음악을 배우고 싶은 사람 누구에게나 쉽게 접근할 수 있는 악기를 만드는 것

- 언제 어디서나 음악으로 자기 자신을 표현할 수 있도록 하는것

- 아날로그적인 악기들로 만든 음악들과 비교해 뒤쳐지지 않는 음악을 할 수 있도록 지원하는 것

- 누구나 쉽게 음악을 만들 수 있도록 하는 것

- 스마트폰만 있으면 몸이 악기가 되고, 이를 통해 자유롭게 음악을 표현할 수 있도록 할것. 





### 3. 구현방법
![image](https://user-images.githubusercontent.com/29995264/135447625-0bd9cd86-85af-43c3-8707-91880b908389.png)
- 플랫폼 : 모바일 app

- 핵심 기술 : **TensorFlow DeepLearning**을 통한 자세 인식 및 **Android API**를 이용한 음악 생성, 언어는 **Kotlin** 및 **JAVA**를 사용

- 사용 툴 : 안드로이드 스튜디오, Github
### 3-1) 텐서플로우 이용

 **Tensorflow**의 **Light Pose Estimation** 샘플은 **Movenet Model**를 사용하는 방법을 보여준다. 

 이 예제를 참고하고 변형해, 사람의 손목과 어깨의 위치를 파악하고 왼손 손목이 어깨로부터 얼마나 떨어져있는지를 계산해 음의 높낮이를 결정한다. 

 손목과 어깨의 거리는 연속적이기 때문에, 음악도 연속적으로 나온다. 

 연속적인 음악을 구현하기 위해 우리는 프로그래밍으로 손목과 어깨 사이의 거리를 음의 높낮이로 환산하여 해당 음을 만들고, 송출할 것이다. 

 왼쪽 손목이 움직일때마다 소리가 나면 매우 불편할것이므로, 오른쪽 손목이 움직일때만 소리가 나게 구현할 것이다. 

### 3-2) 음의 제작

 소리와 같은 경우는 **android.media package**에 포함되어 있는 **SoundPool API**를 활용할 것이다.

 **SoundPool**은 여러 사운드 샘플들을 메모리에 로드하여 소리를 재생할 수 있게 해준다. 

 **SoundPool** 뿐만 아니라 **AudioTrack**, **AudioFormat**을 이용할 것이다. 두 API를 이용하여 주파수, 파형, 음폭을 조절한다.

 이를 통해 여러 다양한 소리를 만들어 연주가 가능하도록 할 것이다.

 간단한 예시로 createSinWaveBuffer( freq,  milisecond,  sampleRate)와 같이 소리의 요소들을 조절하여 소리 샘플 버퍼를 생성한다. 






### 4. 참고자료
Tensorflow 예제 Light Pose Estimation 참고

 -> <https://github.com/tensorflow/examples/tree/master/lite/examples/pose_estimation/android>

Tensorflow Movenet Model

 -> <https://github.com/tensorflow/tfjs-models/tree/master/pose-detection/src/movenet>

SoundPool

 -> <https://developer.android.com/reference/kotlin/android/media/SoundPool>

AudioTrack

 -> <https://developer.android.com/reference/kotlin/android/media/AudioTrack>

AudioFormat

 -> <https://developer.android.com/reference/kotlin/android/media/AudioFormat>

