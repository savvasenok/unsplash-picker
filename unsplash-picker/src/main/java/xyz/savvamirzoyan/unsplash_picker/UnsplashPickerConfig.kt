package xyz.savvamirzoyan.unsplash_picker

object UnsplashPickerConfig {

    internal var ACCESS_KEY = ""
    internal const val PER_PAGE = 50

    fun init(accessKey: String) {
        ACCESS_KEY = accessKey
    }
}