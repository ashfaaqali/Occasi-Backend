package com.occasi.application

import com.occasi.application.config.TestFirebaseConfig
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import

@SpringBootTest
@Import(TestFirebaseConfig::class)
class OccasiApplicationTests {

	@Test
	fun contextLoads() {
	}

}
