package com.example.Trainning.Project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.example.Trainning.Project.repository")
@EntityScan(basePackages = "com.example.Trainning.Project.model")

public class TrainningProjectApplication {

	public static void main(String[] args) {
		SpringApplication.run(TrainningProjectApplication.class, args);
	}

}
