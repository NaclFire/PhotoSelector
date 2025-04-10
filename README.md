## PhotoSelector 照片选择器
仿新浪微博照片选择器
## 效果图
![照片选择器](./Demo1.png)
![照片选择器](./Demo2.png)
![照片选择器](./Demo3.png)
![照片选择器](./Demo4.gif)

##如何使用

1.Gradle添加依赖:

(1)在Project的build.gradle中添加:

	allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}

(2)Module的build.gradle中添加:

	dependencies {
		...
	    implementation 'com.github.NaclFire:PhotoSelector:1.5.3'
	    implementation 'com.github.NaclFire:PhotoSelector:1.5.3X'//AndroidX依赖，与上面二选一
	}


2.在AndroidManifest.xml中添加权限:

	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

3.Java代码:

	在需要开启照片选择器的地方调用:
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

    }