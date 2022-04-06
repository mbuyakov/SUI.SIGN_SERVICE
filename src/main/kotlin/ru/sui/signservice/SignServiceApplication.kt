package ru.sui.signservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class SignServiceApplication

fun main(args: Array<String>) {
    runApplication<SignServiceApplication>(*args)
}
