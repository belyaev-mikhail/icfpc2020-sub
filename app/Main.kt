import java.net.*
import java.net.http.*

fun main(args: Array<String>) {
    val (serverUrl, playerKey) = args

    println("ServerUrl: $serverUrl; PlayerKey: $playerKey")

    val request = HttpRequest.newBuilder()
            .uri(URI.create("$serverUrl?playerKey=$playerKey"))
            .build()

    val response = HttpClient.newHttpClient()
            .send(request, HttpResponse.BodyHandlers.discarding())

    val status = response.statusCode()

    if (status != HttpURLConnection.HTTP_OK) {
        throw RuntimeException("Failed to send request. Status code: $status")
    }
}