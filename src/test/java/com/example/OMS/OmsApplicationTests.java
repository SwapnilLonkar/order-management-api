package com.example.OMS;

import com.example.OMS.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class OmsApplicationTests {

	@Autowired
	private ApplicationContext context;

	@Test
	void contextLoads() {
		// A real boot, not a mocked slice: bean-wiring mistakes (e.g. the
		// P3-9 Jackson 2 vs 3 ObjectMapper mix-up) only fail here, so assert
		// the security beans the app cannot start without are present.
		assertNotNull(context.getBean(JwtService.class));
		assertNotNull(context.getBean(SecurityFilterChain.class));
		assertNotNull(context.getBean(AuthenticationManager.class));
		assertNotNull(context.getBean(PasswordEncoder.class));
		assertTrue(context.containsBean("jwtAuthenticationFilter"));
	}

}
