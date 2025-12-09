package com.occasi.application

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class OccasiApplication

fun main(args: Array<String>) {
	runApplication<OccasiApplication>(*args)
}
