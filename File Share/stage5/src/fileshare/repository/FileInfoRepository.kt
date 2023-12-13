package fileshare.repository

import org.springframework.data.jpa.repository.JpaRepository

interface FileInfoRepository : JpaRepository<FileInfo, Long> {
    fun findFirstByHash(hash: String): FileInfo?
}
