package fileshare.service

import fileshare.repository.FileInfo
import fileshare.repository.FileInfoRepository
import fileshare.service.exception.PayloadTooLargeException
import fileshare.service.exception.UnsupportedMediaTypeException
import fileshare.web.InfoResponse
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.core.io.PathResource
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class FileService(
    @Value("\${uploads.dir}") private val baseDir: String,
    private val repository: FileInfoRepository
) {
    fun save(file: MultipartFile): Long {
        try {
            val freeSpace = 200_000 - getInfo().totalBytes
            println("file size: ${file.size}")
            println("free space: $freeSpace")
            if (freeSpace < file.size) {
                throw PayloadTooLargeException()
            }

            val mediaType = requireNotNull(file.contentType)
            if (!file.bytes.isOfMimeType(mediaType)) {
                throw UnsupportedMediaTypeException()
            }

            val originalFilename = requireNotNull(file.originalFilename)
            val localFilename = UUID.randomUUID().toString()
            val destination = Path.of(baseDir, localFilename)

            file.transferTo(destination)

            val fileInfo = FileInfo(
                originalName = originalFilename,
                mediaType = mediaType,
                localName = destination.toString()
            )

            return requireNotNull(repository.save(fileInfo).id)
        } catch (e: IOException) {
            throw RuntimeException(e)
        }
    }

    fun load(id: String): FileContainer {
        try {
            val fileInfo = repository.findById(id.toLong())
                .orElseThrow { RuntimeException("id=$id, file info not found") }
            val resource = PathResource(Path.of(baseDir, fileInfo.localName))
            if (resource.exists()) {
                return FileContainer(
                    resource = resource,
                    mediaType = fileInfo.mediaType,
                    originalName = fileInfo.originalName
                )
            }
            throw RuntimeException("Resource not found")
        } catch (e: NumberFormatException) {
            throw RuntimeException(e)
        }
    }

    fun getInfo(): InfoResponse {
        val count = AtomicInteger(0)
        val size = AtomicLong(0)
        try {
            Files.walk(Path.of(baseDir)).use {
                it.filter { path -> !path.toFile().isDirectory }
                    .forEach { path ->
                            count.addAndGet(1)
                            size.addAndGet(Files.size(path))
                    }
                return InfoResponse(
                    totalFiles = count.get(),
                    totalBytes = size.get()
                )
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
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
