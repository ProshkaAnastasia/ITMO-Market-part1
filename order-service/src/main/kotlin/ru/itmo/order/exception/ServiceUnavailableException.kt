package ru.itmo.order.exception

class ServiceUnavailableException(
    message: String,
    cause: Throwable? = null
) : RuntimeException(message, cause)
