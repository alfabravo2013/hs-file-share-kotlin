package fileshare.service

import org.springframework.core.io.Resource

data class FileContainer(
    val resource: Resource,
    val mediaType: String,
    val originalName: String
)
