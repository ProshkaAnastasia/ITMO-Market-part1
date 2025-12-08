package ru.itmo.order

import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ActiveProfiles

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class OrderServiceApplicationTests {

    @Test
    fun contextLoads() {
    }

}
