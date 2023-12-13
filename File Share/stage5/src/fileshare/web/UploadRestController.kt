package fileshare.web

import fileshare.service.FileContainer
import fileshare.service.FileService
import org.springframework.core.io.Resource
import org.springframework.http.ContentDisposition
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.net.URI

@RestController
@RequestMapping(path = ["/api/v1"])
class UploadRestController(private val service: FileService) {
    private val maxFileSize = 50_000
    private val baseUrl = "http://localhost:8888/api/v1"

    @GetMapping(path = ["/info"])
    fun info(): ResponseEntity<InfoResponse> {
        try {
            val info = service.getInfo()
            return ResponseEntity.ok().body(info)
        } catch (e: RuntimeException) {
            return ResponseEntity.internalServerError().build()
        }
    }

    @PostMapping(path = ["/upload"])
    fun upload(@RequestParam(name = "file") file: MultipartFile): ResponseEntity<Any> {
        if (file.size > maxFileSize) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).build()
        }

        val id = service.save(file)
        val url = "$baseUrl/download/$id"
        return ResponseEntity.created(URI.create(url)).build()

    }

    @GetMapping(path = ["/download/{id}"])
    fun download(@PathVariable id: String): ResponseEntity<Resource> {
        try {
            val container: FileContainer = service.load(id)
            val headers = HttpHeaders()
            headers.contentDisposition = ContentDisposition.attachment().filename(container.originalName).build()
            headers.contentType = MediaType.parseMediaType(container.mediaType)
            return ResponseEntity.ok().headers(headers).body(container.resource)
        } catch (e: RuntimeException) {
            println(e.message)
            return ResponseEntity.notFound().build()
        }
    }
}
