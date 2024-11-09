# webfunny_otel_android
安卓探针

## 如何使用：

### Step 1. 添加 JitPack repository 到 build 文件中：
在您工程的根 build.gradle 文件的  `repositories` 的尾部添加：

```kotlin
dependencyResolutionManagement {
		repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
		repositories {
			mavenCentral()
			maven { url 'https://jitpack.io' }
		}
	}

```
### Step 2. 添加本Library的依赖：
```kotlin
	dependencies {
	        implementation 'com.github.a597873885:webfunny_otel_android:Tag'
	}
```
其中 Tag，需要用真正的发布出的版本好替代，比如要使用 1.0.0 版本的库，则使用如下代码：
```kotlin
	dependencies {
	        implementation 'com.github.a597873885:webfunny_otel_android:1.0.0'
	}
```

### Step3. 初始化组件

WebFunny探针组件的相关初始化都封装到 `WebfunnyRum` 类里面，其采用Builder模式，可在接入探针的共存的 Application 的 onCreate 函数里进行初始化相关调用。比如：


```kotlin
public class SampleApplication extends Application {
    
    @Override
    public void onCreate() {
        super.onCreate();

        WebfunnyRum.builder()
                // note: for these values to be resolved, put them in your local.properties
                // file as rum.beacon.url and rum.access.token
                .setRealm(getResources().getString(R.string.rum_realm))
                .setApplicationName("Android Demo App")
                .enableDebug()
                .enableDiskBuffering()
                .setDeploymentEnvironment("demo")
                .limitDiskUsageMegabytes(1)
                .setGlobalAttributes(
                        Attributes.builder()
                                .put("vendor", "Webfunny")
                                .put("userId", "手机号")
                                .put("userTag", "用户标签")
                                .put("projectVersion", BuildConfig.VERSION_CODE)
                                .put("env", "dev，pro")
                                .put("customrKey", "生成内置id")
                                .put(StandardAttributes.APP_VERSION, BuildConfig.VERSION_NAME)
                                .build())
                .build(this);
    }
}
```
更多其他参数配置可以参看工程 demo 中的 `SampleApplication`类中的实现。。