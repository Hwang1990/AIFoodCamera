# AIFoodCamera

基于 CameraX + Jetpack Compose 的 AI 食物识别应用。拍照后调用 Qwen3-VL 视觉模型识别食物名称、估算热量，并通过 TTS 朗读结果。

## 功能

- 全屏 CameraX 相机预览
- 底部圆形拍照按钮
- 图片转 Base64 后调用视觉 API
- JSON 解析并展示识别结果
- Android TextToSpeech 朗读结果
- CAMERA / INTERNET 权限请求

## 技术栈

- Kotlin + Gradle Kotlin DSL
- MVVM（ViewModel + StateFlow）
- Jetpack Compose + Material 3
- CameraX
- Retrofit + OkHttp + Gson
- **SiliconFlow** + `Qwen/Qwen3-VL-8B-Instruct`

## API 说明

本项目通过 [SiliconFlow](https://cloud.siliconflow.cn) 调用 **Qwen3-VL-8B-Instruct** 视觉模型（OpenAI 兼容格式）。

> 注意：DeepSeek 官方 Chat API 不支持图片；SiliconFlow 上的 `deepseek-vl2` 也已下线。

## 配置

1. 用 Android Studio 打开项目目录 `AIFoodCamera`
2. 在 [SiliconFlow 控制台](https://cloud.siliconflow.cn/account/ak) 申请 API Key
3. 编辑 `local.properties`：

```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
SILICONFLOW_API_KEY=sk-你的密钥
```

4. Sync Project / Rebuild 后运行

## 运行

- minSdk: 26 (Android 8.0)
- 建议在真机上测试（需要相机和网络）

## 项目结构

```
com.hjun.aifoodcamera/
├── MainActivity.kt
├── data/
│   ├── NetworkModule.kt
│   ├── api/          # VisionApiService + 请求/响应模型
│   ├── model/        # FoodAnalysisResult
│   └── repository/   # FoodAnalysisRepository
├── ui/
│   ├── CameraScreen.kt
│   └── theme/
├── util/
│   ├── ImageUtils.kt
│   └── TextToSpeechHelper.kt
└── viewmodel/
    └── CameraViewModel.kt
```
