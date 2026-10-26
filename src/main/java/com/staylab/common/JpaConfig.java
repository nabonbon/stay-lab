package com.staylab.common;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

// @SpringBootApplication에 붙이면 @WebMvcTest 같은 슬라이스 테스트에서도 JPA 설정을 찾다가 실패하므로 분리
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
