package com.waterpark.tickershow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TickershowApplication {

	public static void main(String[] args) {
		SpringApplication.run(TickershowApplication.class, args);
	}

	@Bean
	public CommandLineRunner fixDatabaseSchema(JdbcTemplate jdbcTemplate) {
		return args -> {
			try {
				jdbcTemplate.execute("ALTER TABLE venues MODIFY capacity INT NULL");
				System.out.println("=====================================================");
				System.out.println(" SUCCESS: ĐÃ TỰ ĐỘNG FIX LỖI DATABASE CHO BẠN! ");
				System.out.println("=====================================================");
			} catch (Exception e) {
				System.out.println("SCHEMA FIX IGNORED: " + e.getMessage());
			}
		};
	}
}
