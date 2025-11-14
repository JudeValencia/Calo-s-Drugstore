package com.inventory.Calo.s_Drugstore;

import com.inventory.Calo.s_Drugstore.service.AuthenticationService;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.beans.factory.annotation.Autowired;

@SpringBootApplication
public class DrugstoreApplication {

    @Autowired
    private AuthenticationService authenticationService;

    public static void main(String[] args) {
        javafx.application.Application.launch(JavaFXApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        authenticationService.createDefaultAdminIfNeeded();
    }
}