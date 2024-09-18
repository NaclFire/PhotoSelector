## PhotoSelector 照片选择器
仿新浪微博照片选择器
## 效果图
![照片选择器](./Demo1.jpg)
![照片选择器](./Demo2.jpg)
![照片选择器](./Demo3.jpg)
![照片选择器](./Demo4.jpg)
![照片选择器](./Demo6.jpg)

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
	    implementation 'com.github.NaclFire:PhotoSelector:1.5.2'
	    implementation 'com.github.NaclFire:PhotoSelector:1.5.2X'//AndroidX依赖，与上面二选一
	}


2.在AndroidManifest.xml中添加权限:

	<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />

3.Java代码:

	在需要开启照片选择器的地方调用:
	private void selectPhotos(int sum, int columnCount) {
        // 最大可选照片数
        PhotoSelectorSetting.MAX_PHOTO_SUM = sum;
        // 照片列表列数
        PhotoSelectorSetting.COLUMN_COUNT = columnCount;
        // 是否显示原图按钮
        PhotoSelectorSetting.IS_SHOW_SELECTED_ORIGINAL_IMAGE = false;
        // 图片list
        PhotoSelectorSetting.SELECTED_PHOTOS = result;
        // 启动图片选择activity
        PhotoSelectorActivity.startMe(this, REQUEST_SELECT_PHOTO);
        // Intent intent = new Intent(MainActivity.this, PhotoSelectorActivity.class);
        // startActivityForResult(intent, REQUEST_SELECT_PHOTO);
    }

	在onActivityResult中接收返回集合:
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQUEST_SELECT_PHOTO:
                if (resultCode == RESULT_OK) {
                    // result为照片绝对路径集合,isSelectedOriginImage标识是否选择原图
                    result = data.getStringArrayListExtra(PhotoSelectorSetting.LAST_MODIFIED_LIST);
                    boolean isSelectedOriginImage = data.getBooleanExtra(PhotoSelectorSetting.SELECTED_ORIGINAL_IMAGE, false);
                    // TODO: 获取照片后的操作
                }
                break;
        }
    }
	
