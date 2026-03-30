package com.cap.admin;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AdminServiceApplication.class, properties = {
	"spring.liquibase.enabled=false",
	"spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1",
	"spring.datasource.driver-class-name=org.h2.Driver"
})
class AdminServiceApplicationTests {

	@Test
	void contextLoads() {
	}

}
