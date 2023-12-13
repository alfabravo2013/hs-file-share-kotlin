package fileshare.web

import com.fasterxml.jackson.annotation.JsonProperty

data class InfoResponse(
    @JsonProperty("total_files")
    val totalFiles: Int,

    @JsonProperty("total_bytes")
    val totalBytes: Long
)
