package com.carpetadigital.ecommerce.dashboard.service;

import com.carpetadigital.ecommerce.Auth.User.User;
import com.carpetadigital.ecommerce.Auth.User.UserRepository;
import com.carpetadigital.ecommerce.entity.Payment;
import com.carpetadigital.ecommerce.entity.Rol;
import com.carpetadigital.ecommerce.entity.dto.PaymentResponse;
import com.carpetadigital.ecommerce.entity.dto.RequesUser;
import com.carpetadigital.ecommerce.entity.dto.UsersResponse;
import com.carpetadigital.ecommerce.utils.enums.RolEnum;
import com.carpetadigital.ecommerce.utils.exception.common.ResourceNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import com.carpetadigital.ecommerce.repository.PaymentRepository;

@Slf4j
@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final PaymentRepository paymentRepository;


    public DashboardService(UserRepository userRepository, PaymentRepository paymentRepository) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
    }

    public List<UsersResponse> getUsers() {
        log.info("Getting users");
        List<User> users = userRepository.findAll();

        List<UsersResponse> usersResponses = users.stream().map(user ->

                new UsersResponse(
                        user.getId(),
                        user.getEmail(),
                        user.getFirstname(),
                        user.getRol().getName(),
                        user.getImage(),
                        user.getCountry(),
                        user.getPaymentCount(),
                        user.getTotalAmountPaid()
                )
        ).collect(Collectors.toList());

        return usersResponses;
    }

    public List<PaymentResponse> getPayments() {
        log.info("Getting payments");
        List<Payment> payments = paymentRepository.findAll();


        return payments.stream().map(payment -> {
            User user = userRepository.findById(payment.getUser().getId()).orElse(null);
            PaymentResponse paymentResponse = new PaymentResponse();
            if (user != null) {
                paymentResponse.setUserId(String.valueOf(user.getId()));
                paymentResponse.setEmail(user.getEmail());
                paymentResponse.setFirstName(user.getFirstname());
            }
            paymentResponse.setPaymentId(String.valueOf(payment.getPaymentId()));
            paymentResponse.setAmount(String.valueOf(payment.getAmount()));
            paymentResponse.setPaymentDate(payment.getPaymentDate().toLocalDateTime());
            paymentResponse.setState(String.valueOf(payment.getState().getStateName()));
            return paymentResponse;
        }).collect(Collectors.toList());

    }

    public boolean updateUser(Long id, RequesUser requesUser) throws Exception {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new Exception("User not found"));

        if (requesUser.getUsername() != null) {
            user.setUsername(requesUser.getUsername());
        }
        if (requesUser.getEmail() != null) {
            user.setEmail(requesUser.getEmail());
        }
        if (requesUser.getFirstname() != null) {
            user.setFirstname(requesUser.getFirstname());
        }
        if (requesUser.getLastname() != null) {
            user.setLastname(requesUser.getLastname());
        }
        if (requesUser.getCountry() != null) {
            user.setCountry(requesUser.getCountry());
        }
        if (requesUser.getImage() != null) {
            user.setImage(requesUser.getImage());
        }
        if (requesUser.getRolId() != null) {
            RolEnum rolEnum = RolEnum.getById(requesUser.getRolId());
            user.setRol(new Rol(rolEnum.getId(), rolEnum.getName()));
        }

        userRepository.save(user);

        return true;
    }

    public boolean deleteUserById(Long userId) {
        User user = userRepository.findById(userId).orElseThrow(() ->
                new ResourceNotFoundException("User not found with ID: " + userId)
        );
        try {

            userRepository.delete(user);
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
