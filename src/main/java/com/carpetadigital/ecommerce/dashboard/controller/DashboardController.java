package com.carpetadigital.ecommerce.dashboard.controller;

import com.carpetadigital.ecommerce.dashboard.service.DashboardService;
import com.carpetadigital.ecommerce.entity.dto.RequesUser;
import com.carpetadigital.ecommerce.utils.handler.ResponseHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/users")
    public ResponseEntity<Object> getUsers() {
        return  ResponseHandler.generateResponse(HttpStatus.OK,
               dashboardService.getUsers(),
                true);
    }

    @GetMapping("/payments")
    public ResponseEntity<Object> getPayment() {
        return  ResponseHandler.generateResponse(HttpStatus.OK,
                dashboardService.getPayments(),
                true);
    }

    @PutMapping("/user/{id}")
    public ResponseEntity<Object> updateUser(@PathVariable Long id, @RequestBody RequesUser user) throws Exception {
        return ResponseHandler.generateResponse(HttpStatus.OK,
                dashboardService.updateUser(id, user),
                true);
    }

    @DeleteMapping("/user/{id}")
    public ResponseEntity<Object> deleteUser(@PathVariable Long id) {
            return ResponseHandler.generateResponse(HttpStatus.OK,
                    dashboardService.deleteUserById(id),
                    true);
    }

}
