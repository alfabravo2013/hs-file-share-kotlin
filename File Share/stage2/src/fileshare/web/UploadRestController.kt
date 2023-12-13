package fileshare.web

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.PathResource
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.net.URI
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@RestController
@RequestMapping(path = ["/api/v1"])
class UploadRestController(@Value("\${uploads.dir}") private val baseDir: String) {
    private val baseUrl = "http://localhost:8888/api/v1"

    @GetMapping(path = ["/info"])
    fun info(): ResponseEntity<InfoResponse> {
        val count = AtomicInteger(0)
        val size = AtomicLong(0)

        try {
            Files.walk(Path.of(baseDir)).use {
                it.filter { path -> !path.toFile().isDirectory }
                    .forEach { path ->
                        try {
                            count.addAndGet(1)
                            size.addAndGet(Files.size(path))
                        } catch (e: IOException) {
                            e.printStackTrace()
                            throw RuntimeException(e)
                        }
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity.internalServerError().build()
        }


        return ResponseEntity.ok().body(InfoResponse(count.get(), size.get()))
    }

    @PostMapping(path = ["/upload"])
    fun upload(@RequestParam(name = "file") file: MultipartFile): ResponseEntity<Any> {
        try {
            val filename = requireNotNull(file.originalFilename)
            val destination = Path.of(baseDir, filename)
            println("Saving file to path $destination")
            file.transferTo(destination)
            val url = baseUrl + "/download/" + URLEncoder.encode(filename, StandardCharsets.UTF_8)
            return ResponseEntity.created(URI.create(url)).build()
        } catch (e: Exception) {
            val message = "Error saving file: ${e.javaClass.simpleName}; ${e.message}"
            println(message)
            return ResponseEntity.badRequest().build()
        }
    }

    @GetMapping(path = ["/download/{url}"])
    fun download(@PathVariable url: String): ResponseEntity<Resource> {
        val filename = url.replace(baseUrl, "")
        val path = Path.of(baseDir, URLDecoder.decode(filename, StandardCharsets.UTF_8))
        println("Fetching file from path $path")
        val resource = PathResource(path)
        if (resource.exists()) {
            return ResponseEntity.ok().body(resource)
        }

        return ResponseEntity.notFound().build()
    }

    @PostConstruct
    fun init() {
        val path = Path.of(baseDir)
        if (!path.toFile().exists()) {
            try {
                Files.createDirectory(path)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
}
