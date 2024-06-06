# Android Unsplash Photo Picker

An Android library to pick single or multiple photos from Unsplash. Supports API 23+

Features:
+ Material 3
+ Adaptive layout
+ Search
+ Single and multiple selection

![1](https://github.com/savvasenok/unsplash-picker/assets/31893797/1628465e-28b1-435d-91ef-ee2223ea87a0)
![2](https://github.com/savvasenok/unsplash-picker/assets/31893797/3791ee0c-851f-40f8-9919-fa10f3c35f81)

## How to use?
### Step 1: Add repository
Add it in your root `build.gradle` at the end of repositories:
```gradle
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        maven { url 'https://jitpack.io' }
    }
}
```
### Step 2: Add dependency
Add dependency to your module:
``` gradle
dependencies {

    // ... other dependencies

    implementation "com.github.savvasenok:unsplash-picker:1.0"
}
```
### Step 3: Initialize library
You can initialize the library from any place in your app, but it is recommended to do it in your `Application` class in `onCreate` like this:
```kotlin
class App : Application() {  

    override fun onCreate() {  
        super.onCreate()

        UnsplashPickerConfig.init("YOUR_UNSPLASH_API_KEY") 
    }
}
```
### Step 4: Open an activity
###### *Sidenote*
The library returns the result as a `List<UnsplashPhoto>` of all selected images, even if selection mode is set to **single**. The data model looks like:
```kotlin
data class UnsplashPhoto(  
    val id: String,  
    val thumb: String,  
    val full: String  
)
```
+ ***id*** ... identifier of this photo by Unsplash
+ ***thumb*** ... thumbnail url
+ ***full*** ... url for the photo in the best possible resolution
#### Start selection flow (XML)
Paste this contract in your **Activity**/**Fragment**/**DialogFragment** like any other contract you would do:
```kotlin
val contract = registerForActivityResult(PickingImageFlowFromUnsplashContract()) { unsplashPhotos ->
    /* your code here */ 
}
```

To actually start the selection flow, launch this contract and specify whether you want user to select single image or an arbitrary amount of them:
```kotlin
// for single image
contract.launch( /* isSingleSelect = */ true)

// for multiple images
contract.launch(/* isSingleSelect = */ false)
```
