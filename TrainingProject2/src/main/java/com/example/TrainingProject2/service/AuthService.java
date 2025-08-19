package com.example.TrainingProject2.service;

import com.example.TrainingProject2.dto.auth.RegisterRequestDto;
import com.example.TrainingProject2.exception.DuplicateEmailException;
import com.example.TrainingProject2.exception.DuplicateUsernameException;
import com.example.TrainingProject2.model.Otp;
import com.example.TrainingProject2.model.Role;
import com.example.TrainingProject2.model.User;
import com.example.TrainingProject2.repository.OtpRepository;
import com.example.TrainingProject2.repository.RoleRepository;
import com.example.TrainingProject2.repository.UserRepository;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private EmailService emailService;

    @Autowired
    private OtpRepository otpRepository;
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);



    public User registerNewUser(RegisterRequestDto registrationDto) {
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new RuntimeException("Tên người dùng đã tồn tại!");
        }

        User newUser = new User();
        newUser.setUsername(registrationDto.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        newUser.setName(registrationDto.getName());
        newUser.setPhoneNumber(registrationDto.getPhoneNumber());
        newUser.setEmail(registrationDto.getEmail());

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không được tìm thấy."));
        newUser.setRole(customerRole);

        return userRepository.save(newUser);
    }
    @Transactional
    public void sendRegistrationOtp(RegisterRequestDto registrationDto){
        logger.info("Attempting to send OTP for registration for username: {}", registrationDto.getUsername());

        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new DuplicateUsernameException("Tên người dùng đã tồn tại!");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new DuplicateEmailException("Email đã được đăng ký!");
        }

        String otp = generateOtp();
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(5);

        Optional<Otp> existingOtp =otpRepository.findByEmail(registrationDto.getEmail());
        Otp otpEntity;
        if (existingOtp.isPresent()) {
            otpEntity = existingOtp.get();
            otpEntity.setOtp(otp);
            otpEntity.setExpiryTime(expiryTime);
            logger.info("Updating existing OTP for email: {}", registrationDto.getEmail());
        } else {
            otpEntity = new Otp();
            otpEntity.setEmail(registrationDto.getEmail());
            otpEntity.setOtp(otp);
            otpEntity.setExpiryTime(expiryTime);
            logger.info("Creating new OTP for email: {}", registrationDto.getEmail());
        }
        otpRepository.save(otpEntity);

        // Gửi OTP qua email
        emailService.sendOtpEmail(registrationDto.getEmail(), otp);
        logger.info("OTP sent successfully to {}", registrationDto.getEmail());


    }

    @Transactional
    public User verifyOtpAndRegisterUser(String email, String otpCode, RegisterRequestDto registrationDto) {
        logger.info("Attempting to verify OTP for email: {}", email);

        // 1. Kiểm tra OTP có tồn tại không
        Otp otpEntity = otpRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Mã OTP không hợp lệ hoặc đã hết hạn. Vui lòng thử lại."));

        // 2. Kiểm tra OTP có khớp không
        if (!otpEntity.getOtp().equals(otpCode)) {
            throw new IllegalArgumentException("Mã OTP không đúng.");
        }

        // 3. Kiểm tra OTP còn hạn không
        if (otpEntity.getExpiryTime().isBefore(LocalDateTime.now())) {
            otpRepository.delete(otpEntity);
            throw new IllegalArgumentException("Mã OTP đã hết hạn. Vui lòng yêu cầu mã mới.");
        }

        // Nếu tất cả kiểm tra đều đúng, tiến hành tạo người dùng
        if (userRepository.existsByUsername(registrationDto.getUsername())) {
            throw new IllegalArgumentException("Tên người dùng đã tồn tại! Vui lòng quay lại bước đăng ký.");
        }
        if (userRepository.existsByEmail(registrationDto.getEmail())) {
            throw new IllegalArgumentException("Email đã được đăng ký! Vui lòng quay lại bước đăng ký.");
        }

        User newUser = new User();
        newUser.setUsername(registrationDto.getUsername());
        newUser.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        newUser.setName(registrationDto.getName());
        newUser.setPhoneNumber(registrationDto.getPhoneNumber());
        newUser.setEmail(registrationDto.getEmail());

        Role customerRole = roleRepository.findByName("CUSTOMER")
                .orElseThrow(() -> new RuntimeException("Lỗi: Vai trò không được tìm thấy."));
        newUser.setRole(customerRole);

        User savedUser = userRepository.save(newUser);
        logger.info("User {} registered successfully.", savedUser.getUsername());

        otpRepository.delete(otpEntity);
        logger.info("OTP for email {} deleted after successful registration.", email);

        return savedUser;
    }

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }

}