package com.example.Trainning.Project.service;

import com.example.Trainning.Project.dto.Customer.UserInfoDto;
import com.example.Trainning.Project.model.Customer;
import com.example.Trainning.Project.model.User;
import com.example.Trainning.Project.repository.CustomerRepository;
import com.example.Trainning.Project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    /**
     * Lấy thông tin chi tiết của người dùng hiện đang đăng nhập.
     * @return UserInfoDto chứa tên đăng nhập, số điện thoại, tên đầy đủ và vai trò của người dùng.
     */
    @Transactional(readOnly = true)
    public Optional<UserInfoDto> getCurrentUserInformation() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {

            return Optional.empty();
        }

        String username = authentication.getName();

        Optional<User> userOptional = userRepository.findByPhoneNumber(username);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            UserInfoDto userInfo = new UserInfoDto();
            userInfo.setUsername(user.getPhoneNumber());
            userInfo.setPhoneNumber(user.getPhoneNumber());

            if (user.getRole() == com.example.Trainning.Project.enums.Role.customer) {
                Optional<Customer> customerOptional = customerRepository.findByUser(user);customerOptional.ifPresent(customer -> {
                    userInfo.setFullName(customer.getFullName());
                    userInfo.setCustomerId(customer.getId());
                });
            } else {
                userInfo.setFullName(user.getPhoneNumber());
            }
            userInfo.setRole(user.getRole().name());
            System.out.println("==> authentication: " + authentication);
            System.out.println("==> isAuthenticated: " + authentication.isAuthenticated());
            System.out.println("==> principal: " + authentication.getPrincipal());
            System.out.println("==> getName: " + authentication.getName());
            return Optional.of(userInfo);
        }


        return Optional.empty();
    }
    @Transactional(readOnly = true)
    public boolean isCurrentUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}