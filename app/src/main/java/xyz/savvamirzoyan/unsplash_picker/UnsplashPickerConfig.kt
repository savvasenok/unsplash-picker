package xyz.savvamirzoyan.unsplash_picker

object UnsplashPickerConfig {

    internal var ACCESS_KEY = ""
    internal const val PER_PAGE = 20

    fun init(accessKey: String) {
        ACCESS_KEY = accessKey
    }
}