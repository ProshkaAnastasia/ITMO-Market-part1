package ru.itmo.market

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class ItmoMarketApplication

fun main(args: Array<String>) {
    runApplication<ItmoMarketApplication>(*args)
}
