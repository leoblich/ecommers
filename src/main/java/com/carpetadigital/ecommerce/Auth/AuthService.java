package com.carpetadigital.ecommerce.Auth;

import com.carpetadigital.ecommerce.Auth.Jwt.JwtService;
import com.carpetadigital.ecommerce.Auth.User.User;
import com.carpetadigital.ecommerce.Auth.User.UserRepository;
import com.carpetadigital.ecommerce.email.service.EmailService;
import com.carpetadigital.ecommerce.entity.Rol;
import com.carpetadigital.ecommerce.entity.dto.EmailDto;
import com.carpetadigital.ecommerce.utils.exception.common.ResourceNotFoundException;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2UserAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final RoleService rolService;
    private final AuthenticationManager authenticationManager;
    private final EmailService emailService;

    @Value("${spring.security.oauth2.client.registration.google.client-id}")
    private String googleClientId;

    public AuthResponse login(LoginRequest request) {
        log.info("Autenticando usuario: {}", request);
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));
        User user = userRepository.findByUsername(request.getUsername()).orElseThrow();
        String token = jwtService.getToken(user, user.getFirstname(), user.getLastname(), user.getImage(), user.getRol().getId());
        String refreshToken = jwtService.generateRefreshToken(user);
        return AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .build();
    }

    public AuthResponse register(RegisterRequest request) {
        log.info("Registrando usuario: {}", request);

        // Obtener el rol de la base de datos
        Rol userRole = rolService.findByName(request.getRol());

        if (userRole == null) {
            throw new RuntimeException("Rol no encontrado");
        }

        // Crear un nuevo usuario y asignar el rol
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .firstname(request.getFirstname())
                .lastname(request.getLastname())
                .country(request.getCountry())
                .email(request.getEmail())
                .rol(userRole) // Asignar el rol al usuario
                .build();
        log.info("user " + user);

        // Guardar el usuario
        User savedUser = userRepository.save(user);

        if (savedUser == null) {
            throw new RuntimeException("Error al guardar el usuario");
        }

        EmailDto dataEmail = new EmailDto();
        dataEmail.setTypeTemplate(4);
        dataEmail.setUserId(savedUser.getId()); // Set the user ID

        new Thread(() -> {
            try {
                emailService.sendEmailBasedOnType(dataEmail);
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }).start();

        // Devolver la respuesta de autenticación
        return AuthResponse.builder()
                .token(jwtService.getToken(savedUser, savedUser.getFirstname(), savedUser.getLastname(), savedUser.getImage(), savedUser.getRol().getId()))
                .build();
    }

    public void logout(String token) {
        log.info("Invalidando token: {}" + token);
        // Aquí puedes invalidar el token JWT, por ejemplo, añadiéndolo a una lista de tokens inválidos
        jwtService.invalidateToken(token);
    }

    public String authenticateWithGoogle(Map<String, String> request) {
        log.info("Request: {}", request);
        String idToken = request.get("token");
        try {
            log.info("Verifying Google token: {}", idToken);
            OAuth2User oauth2User = verifyGoogleToken(idToken);
            AuthResponse authResponse = processOAuth2User(oauth2User);
            return  authResponse.getToken();
        } catch (Exception e) {
             throw new ResourceNotFoundException("Invalid ID token.");

        }
    }

    public OAuth2User verifyGoogleToken(String idTokenString) {
        try {
            log.info("Verifying Google token: {}", idTokenString);
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(GoogleNetHttpTransport.newTrustedTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(googleClientId))
                    .build();
log.info("verifier " + verifier);
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken != null) {
                GoogleIdToken.Payload payload = idToken.getPayload();
                Map<String, Object> attributes = payload;
                return new DefaultOAuth2User(Collections.singleton(new OAuth2UserAuthority(attributes)), attributes, "sub");
            } else {
                throw new RuntimeException("Invalid ID token.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to verify ID token.", e);
        }
    }


    public AuthResponse processOAuth2User(OAuth2User oauth2User) {
        // Extract user information from OAuth2User
        String email = oauth2User.getAttribute("email");
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            // Create a new user if not found
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setFirstname(oauth2User.getAttribute("given_name"));
            newUser.setLastname(oauth2User.getAttribute("family_name"));
            newUser.setUsername(email);
            newUser.setImage(oauth2User.getAttribute("picture"));
            newUser.setPassword(passwordEncoder.encode("oauth2user")); // Set a default password
            newUser.setRol(rolService.findByName("USER")); // Assign default role

            User savedUser = userRepository.save(newUser);

            // Send email with user ID
            EmailDto dataEmail = new EmailDto();
            dataEmail.setTypeTemplate(4);
            dataEmail.setUserId(savedUser.getId()); // Set the user ID

            new Thread(() -> {
                try {
                    emailService.sendEmailBasedOnType(dataEmail);
                } catch (MessagingException e) {
                    throw new RuntimeException(e);
                }
            }).start();

            return savedUser;
        });

        // Generate JWT token
        String token = jwtService.getToken(user, user.getFirstname(), user.getLastname(), user.getImage(), user.getRol().getId());
        return AuthResponse.builder()
                .token(token)
                .build();
    }

    public AuthResponse refreshToken(String refreshToken) {
        if (jwtService.isTokenValid(refreshToken, null)) {
            String username = jwtService.getUsernameFromToken(refreshToken);
            User user = userRepository.findByUsername(username).orElseThrow();
            String newToken = jwtService.getToken(user, user.getFirstname(), user.getLastname(), user.getImage(), user.getRol().getId());
            return AuthResponse.builder()
                    .token(newToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            throw new RuntimeException("Invalid refresh token");
        }
    }
}

