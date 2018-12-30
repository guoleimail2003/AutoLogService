1. How to download the Android studio?
These source code is used in Android Studio. You can download Android Studio in this website http://www.android-studio.org/
and download the SDK tool http://tools.android-studio.org/index.php/sdk.

2. How to use the source code in Android Studio?
   1. Open Android studio -> File -> New Project.
   2. According to the guide and setup a new project, there is an APK in this project.
   3. Access the diretory(C:\Users\xxxxx\AndroidStudioProjects\ProjectName\app)
   4. Delete the src foder in the Project directory(C:\Users\xxxxx\AndroidStudioProjects\ProjectName\app\src)
   5. Copy the AutoLogService src folder to the Project Source code.
   6. Modify the build.gradle in the app folder.
   
   we should add the following source code in build.gradle   



String SDK_DIR = System.getenv("ANDROID_SDK_HOME")


if (SDK_DIR == null) {

    Properties props = new Properties()
    
    props.load(new FileInputStream(project.rootProject.file("local.properties")))
    
    SDK_DIR = props.get('sdk.dir');
}


dependencies {

    implementation files(SDK_DIR + "\\platforms\\android-21\\data\\layoutlib.jar");

    implementation files('libs/org.eclipse.paho.client.mqttv3.jar')
    
    implementation 'com.android.support:support-annotations:21.0.3'
}
