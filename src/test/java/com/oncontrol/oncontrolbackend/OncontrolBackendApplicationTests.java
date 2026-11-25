package com.oncontrol.oncontrolbackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = "spring.datasource.url=jdbc:h2:mem:context-smoke;MODE=MySQL")
@ActiveProfiles("test")
class OncontrolBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
