package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.Auth.AuthRequest;
import com.example.Trainning.Project.dto.Auth.AuthResponse;
import com.example.Trainning.Project.dto.Customer.CustomerRegistrationRequest;
import com.example.Trainning.Project.enums.Role;
import com.example.Trainning.Project.exception.EmailAlreadyExistsException; // Import
import com.example.Trainning.Project.exception.PhoneNumberAlreadyExistsException; // Import
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.User;
import com.example.Trainning.Project.repository.CustomerRepository;
import com.example.Trainning.Project.repository.UserRepository;
import com.example.Trainning.Project.security.JwtTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Transactional
    public AuthResponse authenticateUser(AuthRequest authRequest) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(authRequest.getPhoneNumber(), authRequest.getPassword())
        );

        final UserDetails userDetails = userRepository
                .findByPhoneNumber(authRequest.getPhoneNumber())
                .orElseThrow(() -> new BadCredentialsException("Người dùng không tìm thấy sau xác thực. Lỗi logic!"));

        final String jwt = jwtTokenUtil.generateToken(userDetails);
        return new AuthResponse(jwt);
    }

    @Transactional
    public String registerNewCustomer(CustomerRegistrationRequest registerRequest) {
        // 1. Kiểm tra số điện thoại đã tồn tại chưa
        if (userRepository.findByPhoneNumber(registerRequest.getPhoneNumber()).isPresent()) {
            throw new PhoneNumberAlreadyExistsException("Số điện thoại '" + registerRequest.getPhoneNumber() + "' đã được sử dụng.");
        }

        // 2. Kiểm tra email đã tồn tại chưa (Bạn cần thêm CustomerRepository.findByEmail)
        if (customerRepository.findByEmail(registerRequest.getEmail()).isPresent()) {
            throw new EmailAlreadyExistsException("Email '" + registerRequest.getEmail() + "' đã được sử dụng.");
        }

        // 3. Tạo User mới
        User newUser = new User();
        newUser.setPhoneNumber(registerRequest.getPhoneNumber());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole(Role.customer);

        // 4. Lưu User vào database
        User savedUser = userRepository.save(newUser);

        // 5. Tạo Customer mới và liên kết với User
        Customer newCustomer = new Customer();
        newCustomer.setUser(savedUser);
        newCustomer.setFullName(registerRequest.getFullName());
        newCustomer.setEmail(registerRequest.getEmail());
        newCustomer.setAddress(registerRequest.getAddress());

        // 6. Lưu Customer vào database
        customerRepository.save(newCustomer);

        return "Người dùng và khách hàng đã đăng ký thành công!";
    }
}