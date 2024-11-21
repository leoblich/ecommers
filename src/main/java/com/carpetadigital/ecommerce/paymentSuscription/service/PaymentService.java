package com.carpetadigital.ecommerce.paymentSuscription.service;


import com.carpetadigital.ecommerce.Auth.User.User;
import com.carpetadigital.ecommerce.Auth.User.UserRepository;
import com.carpetadigital.ecommerce.email.service.EmailService;

import com.carpetadigital.ecommerce.Repository.StateRepository;
import com.carpetadigital.ecommerce.entity.DocumentsEntity;

import com.carpetadigital.ecommerce.entity.Payment;
import com.carpetadigital.ecommerce.entity.State;
import com.carpetadigital.ecommerce.entity.Subscription;
import com.carpetadigital.ecommerce.entity.dto.EmailDto;
import com.carpetadigital.ecommerce.entity.dto.PaymentSuscriptionDto;
import com.carpetadigital.ecommerce.repository.PaymentRepository;
import com.carpetadigital.ecommerce.repository.SubscriptionRepository;

import com.carpetadigital.ecommerce.utils.exception.common.ResourceNotFoundException;
import jakarta.mail.MessagingException;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class PaymentService {

    private static final Long DEFAULT_STATE_ID = 2L;

    private final PaymentRepository paymentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final EmailService emailService;
    private final StateRepository stateRepository;
    private final UserRepository userRepository;
    private final com.carpetadigital.ecommerce.Repository.DocumentsRepository documentsRepository;

    @Autowired
    public PaymentService(PaymentRepository paymentRepository,
                          SubscriptionRepository subscriptionRepository, EmailService emailService, StateRepository stateRepository, UserRepository userRepository,
                          com.carpetadigital.ecommerce.Repository.DocumentsRepository documentsRepository) {
        this.paymentRepository = paymentRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.emailService = emailService;
        this.stateRepository = stateRepository;
        this.userRepository = userRepository;
        this.documentsRepository = documentsRepository;
    }

    @Transactional
    public Boolean processPayment(PaymentSuscriptionDto paymentSuscriptionDto) throws Exception {
        log.info("Processing payment: {}", paymentSuscriptionDto);

        validatePayment(paymentSuscriptionDto);

        Payment payment = createPayment(paymentSuscriptionDto);
        Payment savedPayment = paymentRepository.save(payment);

        if (payment.isSubscription()) {
            processSubscriptionPayment(paymentSuscriptionDto, payment);
        } else {
            processOrderPayment(paymentSuscriptionDto, payment);
        }


        return true;
    }

    private void validatePayment(PaymentSuscriptionDto paymentSuscriptionDto) throws Exception {
        if (paymentSuscriptionDto.getStatus() == null) {
            throw new Exception("Payment was not successful");
        }

    }

    private Payment createPayment(PaymentSuscriptionDto paymentSuscriptionDto) {

        Payment payment = new Payment();

        if (paymentSuscriptionDto.getUserId() != null) {
            Optional<User> userOptional = userRepository.findById(paymentSuscriptionDto.getUserId());
            if (userOptional.isEmpty()) {
                throw new ResourceNotFoundException("User not found");
            }
            payment.setUser(userOptional.get());
        }else {
            User user = new User();
            user.setEmail(paymentSuscriptionDto.getGuestEmail());
            payment.setUser(user);
        }


        payment.setPaymentDate(new Timestamp(System.currentTimeMillis()));
        payment.getUser().setId(paymentSuscriptionDto.getUserId());
        payment.setAmount(paymentSuscriptionDto.getAmount());
        payment.setPaymentStatus(paymentSuscriptionDto.getStatus());
        payment.setIsSubscription(paymentSuscriptionDto.isSubscription());

        State defaultState = getDefaultState();
        payment.setState(defaultState);

        return payment;
    }

    private void processSubscriptionPayment(PaymentSuscriptionDto paymentSuscriptionDto, Payment payment) throws MessagingException {
        Subscription subscription = createSubscription(paymentSuscriptionDto);
        Subscription savedSubscription = subscriptionRepository.save(subscription);
        payment.setSubscription(savedSubscription);

        EmailDto dataEmail = mapperPaymentToEmailDto( payment);
        dataEmail.setTypeTemplate(1);
        dataEmail.setVoucherNumber(Math.toIntExact(payment.getPaymentId()));
        dataEmail.setSubscriptionType(paymentSuscriptionDto.getSubscriptionType());


        new Thread(() -> {
            try {
                emailService.sendEmailBasedOnType(dataEmail);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).start();



        log.info("Subscription successful: {}", subscription);
    }

    private Subscription createSubscription(PaymentSuscriptionDto paymentSuscriptionDto) {
        Subscription subscription = new Subscription();
        subscription.setUserId(paymentSuscriptionDto.getUserId());
        subscription.setSubscriptionType(paymentSuscriptionDto.getSubscriptionType());

        Date sqlStartDate = new Date(System.currentTimeMillis());
        subscription.setStartDate(sqlStartDate);

        LocalDate startLocalDate = sqlStartDate.toLocalDate();
        LocalDate endLocalDate = startLocalDate.plusMonths(12);
        Date sqlEndDate = Date.valueOf(endLocalDate);

        subscription.setEndDate(sqlEndDate);

        State defaultState = getDefaultState();
        subscription.setState(defaultState);

        return subscription;
    }

    private void processOrderPayment(PaymentSuscriptionDto paymentSuscriptionDto, Payment payment) throws Exception {




        List<DocumentsEntity> documents = documentsRepository.findAllById(paymentSuscriptionDto.getDocumentIds());

        verifyDocumentsAssociation(documents, payment);

        payment.setDocuments(documents);
        Payment newPayment = paymentRepository.save(payment);

        EmailDto dataEmail = mapperPaymentToEmailDto( newPayment);
        dataEmail.setDownloadUrl("http://localhost:8080/api/v1/documents/download/");

        if (paymentSuscriptionDto.getUserId() != null) {
            dataEmail.setTypeTemplate(2);
            dataEmail.setUserId(paymentSuscriptionDto.getUserId());
        } else {
            dataEmail.setGuestEmail(paymentSuscriptionDto.getGuestEmail());
            dataEmail.setTypeTemplate(3);
        }
        new Thread(() -> {
            try {
                emailService.sendEmailBasedOnType(dataEmail);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).start();
        log.info("Order successful: {}", payment);
    }

    private void verifyDocumentsAssociation(List<DocumentsEntity> documents, Payment payment) throws Exception {
        for (DocumentsEntity document : documents) {
            if (document.getPayments().contains(payment)) {
                throw new Exception("El documento ya está asociado con el pago");
            }
        }
    }

    private State getDefaultState() {
        return stateRepository.findById(DEFAULT_STATE_ID)
                .orElseThrow(() -> new IllegalStateException("Default state not found"));
    }

    private EmailDto mapperPaymentToEmailDto(Payment payment) {
        EmailDto emailDto = new EmailDto();
        emailDto.setAmount(payment.getAmount());
        emailDto.setVoucherNumber (Math.toIntExact(payment.getPaymentId()));
        emailDto.setUserId(payment.getUser().getId());
        return emailDto;
    }
}