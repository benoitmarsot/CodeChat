package com.unbumpkin.codechat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class CodechatApplication {

	public static void main(String[] args) {
		SpringApplication.run(CodechatApplication.class, args);
		System.out.println("CodechatApplication started successfully");
		System.out.println("Java version: " + System.getProperty("java.version"));
		System.out.println("OS name: " + System.getProperty("os.name"));
		System.out.println("OS version: " + System.getProperty("os.version"));
		
	}
}
