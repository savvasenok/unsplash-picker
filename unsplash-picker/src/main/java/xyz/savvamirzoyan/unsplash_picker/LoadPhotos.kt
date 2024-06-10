package xyz.savvamirzoyan.unsplash_picker

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import xyz.savvamirzoyan.unsplash_picker.model.UnsplashPhoto
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

internal suspend fun loadPhotos(page: Int): List<UnsplashPhoto> = withContext(Dispatchers.IO) {
    var urlConnection: HttpURLConnection? = null

    try {
        val url =
            URL("https://api.unsplash.com/collections/317099/photos?client_id=${UnsplashPickerConfig.ACCESS_KEY}&page=$page&per_page=${UnsplashPickerConfig.PER_PAGE}")

        urlConnection = url.openConnection() as HttpURLConnection

        // Set up the connection properties
        urlConnection.requestMethod = "GET"

        // Read the response using coroutines
        val inputStream = urlConnection.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()

        var line: String? = reader.readLine()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = reader.readLine()
        }

        return@withContext parseJsonToUnsplashPhotos(stringBuilder.toString())
    } catch (e: Exception) {
        return@withContext emptyList()
    } finally {
        urlConnection?.disconnect()
    }
}

internal suspend fun loadPhotos(page: Int, searchQuery: String): List<UnsplashPhoto> = withContext(Dispatchers.IO) {

    var urlConnection: HttpURLConnection? = null

    try {
        val url =
            URL("https://api.unsplash.com/search/photos?client_id=${UnsplashPickerConfig.ACCESS_KEY}&page=$page&per_page=${UnsplashPickerConfig.PER_PAGE}&query=$searchQuery")

        urlConnection = url.openConnection() as HttpURLConnection

        // Set up the connection properties
        urlConnection.requestMethod = "GET"

        // Read the response using coroutines
        val inputStream = urlConnection.inputStream
        val reader = BufferedReader(InputStreamReader(inputStream))
        val stringBuilder = StringBuilder()

        var line: String? = reader.readLine()
        while (line != null) {
            stringBuilder.append(line).append("\n")
            line = reader.readLine()
        }

        val jsonString = JSONObject(stringBuilder.toString()).getJSONArray("results").toString()
        return@withContext parseJsonToUnsplashPhotos(jsonString)
    } catch (e: Exception) {
        return@withContext emptyList()
    } finally {
        urlConnection?.disconnect()
    }
}

private fun parseJsonToUnsplashPhotos(json: String): List<UnsplashPhoto> {
    val array = JSONArray(json)
    val photosList = mutableListOf<UnsplashPhoto>()

    for (i in 0 until array.length()) {
        val jsonObject = array.getJSONObject(i)
        val id = jsonObject.getString("id")
        val urlsObject = jsonObject.getJSONObject("urls")
        val thumbUrl = urlsObject.getString("regular")
        val fullUrl = urlsObject.getString("full")

        val unsplashPhoto = UnsplashPhoto(id, thumbUrl, fullUrl)
        photosList.add(unsplashPhoto)
    }

    return photosList.toList()
}