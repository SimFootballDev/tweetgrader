package com.nsfl.tweetgrader

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@SpringBootApplication
class TweetGraderApplication {

    @RequestMapping
    fun test(): String {
        return "Hello"
    }
}

fun main(args: Array<String>) {
    runApplication<TweetGraderApplication>(*args)
}