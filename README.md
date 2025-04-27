## PhotoSelector 照片选择器
仿新浪微博照片选择器
再看看一个简单的相机拍照录像的工具：[CameraX](https://github.com/NaclFire/CameraX)
## 效果图
![照片选择器](./Demo1.png)
![照片选择器](./Demo2.png)
![照片选择器](./Demo3.png)
![照片选择器](./Demo4.gif)

## 如何使用

1.Gradle添加依赖:

(1)在Project的build.gradle中添加:

```groovy
allprojects {
	repositories {
		...
		maven { url 'https://jitpack.io' }
	}
}
```

(2)Module的build.gradle中添加:

```groovy
dependencies {
	...
    implementation 'com.github.NaclFire:PhotoSelector:1.5.3'
    implementation 'com.github.NaclFire:PhotoSelector:1.6.1X'//AndroidX依赖，与上面二选一
}
```
2.在AndroidManifest.xml中添加权限:

(1)1.5.3版本：

```xml
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

(2)1.6.1X以上版本：

```xml
// 如果应用SDK版本大于33，需要申请READ_MEDIA_IMAGES权限
<uses-permission android:name="android.permission.READ_MEDIA_IMAGES" />
// 如果应用SDK版本小于33，仅需要申请下面的权限
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

3.Java代码:

```java
// 1.5.3版本
// 在需要开启照片选择器的地方调用:
private void selectPhotos(int sum, int columnCount) {
        new PhotoSelectorActivity.Builder()
        // 图片list
        .setSelectedPhotos(result)
        // 最大可选照片数
        .setMaxPhotoSum(sum)
        // 照片列表列数
        .setColumnCount(columnCount)
        // 是否显示原图按钮
        .setShowSelectOrigin(true)
        .setOnPhotoSelectedCallback(new PhotoSelectorActivity.OnPhotoSelectedCallback() {
@Override
public void onPhotoSelected(List<String> photoList, boolean isSelectOrigin) {
        // result为照片绝对路径集合,isSelectOrigin为是否选择原图
        result = (ArrayList<String>) photoList;
        }
        })
        .build(this);
        
// 1.6.1X以上版本        
// 在需要开启照片选择器的地方调用:
private void selectPhotos(int sum, int columnCount) {
    new PhotoSelectorActivity.Builder()
            // 图片集合
            .setSelectedPhotos(result)
            // 最大可选照片数
            .setMaxPhotoSum(sum)
            // 照片列表列数
            .setColumnCount(columnCount)
            // 是否显示原图按钮
            .setShowSelectOrigin(true)
            .setOnPhotoSelectedCallback(new PhotoSelectorActivity.OnPhotoSelectedCallback() {
                @Override
                public void onPhotoSelected(List<ImagePathBean> photoList, boolean isSelectOrigin) {
                    // result为照片集合,isSelectOrigin为是否选择原图
                    // ImagePathBean中getPath为图片绝对路径，getUri为图片Uri
                    result = (ArrayList<ImagePathBean>) photoList;
                }
            })
            .build(this);

}
```
