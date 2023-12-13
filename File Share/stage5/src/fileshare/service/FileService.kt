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
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.math.BigInteger
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.*
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

@Service
class FileService(
    @Value("\${uploads.dir}") private val baseDir: String,
    private val repository: FileInfoRepository
) {

    @Transactional
    fun save(file: MultipartFile): Long {
        try {
            val md5hash = file.bytes.md5hash()
            val originalFilename = requireNotNull(file.originalFilename)
            val mediaType = requireNotNull(file.contentType)
            val localFilename = UUID.randomUUID().toString()

            val existingFileInfo = repository.findFirstByHash(md5hash)

            val fileInfo = FileInfo(
                originalName = originalFilename,
                mediaType = mediaType,
                localName = localFilename,
                hash = md5hash
            )

            if (existingFileInfo != null) {
                if (originalFilename == existingFileInfo.originalName
                    && md5hash == existingFileInfo.hash) {
                    return requireNotNull(existingFileInfo.id)
                }

                fileInfo.localName = existingFileInfo.localName
                repository.save(fileInfo)
            } else {
                val freeSpace = 200_000 - getInfo().totalBytes
                if (freeSpace < file.size) {
                    throw PayloadTooLargeException()
                }

                if (!file.bytes.isOfMimeType(mediaType)) {
                    throw UnsupportedMediaTypeException()
                }

                repository.save(fileInfo)

                val destination = Path.of(baseDir, localFilename)
                file.transferTo(destination)
            }

            return requireNotNull(fileInfo.id)

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

    private fun ByteArray.md5hash(): String {
        try {
            val md = MessageDigest.getInstance("MD5")
            val hash = md.digest(this)
            return BigInteger(-1, hash).toString(16)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        }
    }
}
