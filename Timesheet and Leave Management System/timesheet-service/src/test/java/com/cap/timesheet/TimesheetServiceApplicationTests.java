package com.cap.timesheet;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest(classes = TimesheetServiceApplication.class)
class TimesheetServiceApplicationTests {

	@Test
	void contextLoads() {}

}
