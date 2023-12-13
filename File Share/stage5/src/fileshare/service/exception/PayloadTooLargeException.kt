package fileshare.service.exception

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.ResponseStatus

@ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
class PayloadTooLargeException : RuntimeException()
