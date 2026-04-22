package dev.ravi.dlq;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class DlqMonitorApp {
    public static void main(String[] args) {
        SpringApplication.run(DlqMonitorApp.class, args);
    }
}
