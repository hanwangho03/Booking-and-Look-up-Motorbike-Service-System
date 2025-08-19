package com.example.TrainingProject2.controller;

import com.example.TrainingProject2.dto.auth.OtpVerificationDto;
import com.example.TrainingProject2.dto.auth.RegisterRequestDto;
import com.example.TrainingProject2.dto.user.LoggedUserDto;
import com.example.TrainingProject2.exception.DuplicateEmailException;
import com.example.TrainingProject2.exception.DuplicateUsernameException;
import com.example.TrainingProject2.repository.RoleRepository;
import com.example.TrainingProject2.repository.UserRepository;
import com.example.TrainingProject2.service.AuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @GetMapping("/login")
    public String showLoginPage() {
        return "auth/login";
    }

    @GetMapping("/register")
    public String showRegistrationPage(Model model) {
        model.addAttribute("registrationDto", new RegisterRequestDto());
        return "auth/register";
    }

//    @PostMapping("/register")
//    public String registerUser(@ModelAttribute("user") @Valid RegisterRequestDto userDto, BindingResult result, Model model) {
//        if (result.hasErrors()) {
//            return "/auth/register";
//        }
//        try {
//            authService.registerNewUser(userDto);
//        } catch (RuntimeException e) {
//            model.addAttribute("error", e.getMessage());
//            return "/auth/register";
//        }
//        return "redirect:/login?success=true";
//    }

    // --- XỬ LÝ ĐĂNG KÝ VÀ GỬI OTP (BƯỚC 1) ---
    @PostMapping("/register")
    public String registerUser(@Valid @ModelAttribute("registrationDto") RegisterRequestDto registrationDto,
                               BindingResult result,
                               RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/register";
        }

        try {
            authService.sendRegistrationOtp(registrationDto);
            // Lưu thông tin đăng ký vào flash attributes để truyền sang trang xác minh OTP
            redirectAttributes.addFlashAttribute("registrationDto", registrationDto);
            redirectAttributes.addFlashAttribute("message", "Mã xác thực OTP đã được gửi đến email của bạn. Vui lòng kiểm tra email và nhập mã để hoàn tất đăng ký.");
            return "redirect:/verify-otp"; // Chuyển hướng đến trang xác minh OTP
        } catch (DuplicateUsernameException e) {
            result.rejectValue("username", "error.registrationDto", e.getMessage());
            return "auth/register";
        } catch (DuplicateEmailException e) {
            result.rejectValue("email", "error.registrationDto", e.getMessage());
            return "auth/register";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra: " + e.getMessage()); // Thông báo chung cho các lỗi không mong muốn
            return "redirect:/register";
        }
    }

    // --- HIỂN THỊ TRANG XÁC MINH OTP (BƯỚC 2) ---
    @GetMapping("/verify-otp")
    public String showOtpVerificationForm(
            @ModelAttribute(name = "registrationDto") RegisterRequestDto registrationDto, // Đánh dấu là không bắt buộc
            @ModelAttribute(name = "otpVerificationDto") OtpVerificationDto otpVerificationDtoFlash, // DTO từ flash
            Model model
    ) {
        OtpVerificationDto currentOtpVerificationDto = null;
        if (otpVerificationDtoFlash != null && otpVerificationDtoFlash.getEmail() != null) {
            currentOtpVerificationDto = otpVerificationDtoFlash;
        } else if (registrationDto != null && registrationDto.getEmail() != null) {
            currentOtpVerificationDto = new OtpVerificationDto();
            currentOtpVerificationDto.setEmail(registrationDto.getEmail());
            currentOtpVerificationDto.setUsername(registrationDto.getUsername());
            currentOtpVerificationDto.setPassword(registrationDto.getPassword());
            currentOtpVerificationDto.setConfirmPassword(registrationDto.getConfirmPassword());
            currentOtpVerificationDto.setName(registrationDto.getName());
            currentOtpVerificationDto.setPhoneNumber(registrationDto.getPhoneNumber());
        }

        // Nếu không có dữ liệu nào, chuyển hướng về đăng ký
        if (currentOtpVerificationDto == null || currentOtpVerificationDto.getEmail() == null) {
            logger.warn("showOtpVerificationForm: No valid DTO or email found, redirecting to /register.");
            return "redirect:/register";
        }

        model.addAttribute("otpVerificationDto", currentOtpVerificationDto);
        return "auth/verify-otp";
    }

    // --- XỬ LÝ XÁC MINH OTP VÀ TẠO NGƯỜI DÙNG (BƯỚC 2) ---
    @PostMapping("/verify-otp")
    public String verifyOtp(@Valid @ModelAttribute("otpVerificationDto") OtpVerificationDto otpVerificationDto,
                            BindingResult result,
                            RedirectAttributes redirectAttributes) {

        if (result.hasErrors()) {
            return "auth/verify-otp";
        }

        // Tái tạo RegisterRequestDto từ OtpVerificationDto để truyền cho service
        RegisterRequestDto registrationDto = new RegisterRequestDto();
        registrationDto.setUsername(otpVerificationDto.getUsername());
        registrationDto.setPassword(otpVerificationDto.getPassword());
        registrationDto.setConfirmPassword(otpVerificationDto.getConfirmPassword());
        registrationDto.setName(otpVerificationDto.getName());
        registrationDto.setPhoneNumber(otpVerificationDto.getPhoneNumber());
        registrationDto.setEmail(otpVerificationDto.getEmail());


        try {
            // Xác minh OTP và tạo người dùng
            authService.verifyOtpAndRegisterUser(otpVerificationDto.getEmail(), otpVerificationDto.getOtpCode(), registrationDto);
            redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Bạn có thể đăng nhập ngay bây giờ.");
            return "redirect:/login";
        } catch (IllegalArgumentException e) {
            result.rejectValue("otpCode", "error.otpVerificationDto", e.getMessage());
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "auth/verify-otp";
        } catch (RuntimeException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/verify-otp";
        }
    }

    @PostMapping("/resend-otp")
    public String resendOtp(@ModelAttribute("otpVerificationDto") OtpVerificationDto otpVerificationDto,
                            RedirectAttributes redirectAttributes) {
        if (otpVerificationDto.getEmail() == null || otpVerificationDto.getEmail().isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Email không hợp lệ để gửi lại OTP.");
            return "redirect:/register";
        }
        try {
            // Tạo một RegisterRequestDto tạm thời để gửi lại OTP
            RegisterRequestDto tempRegistrationDto = new RegisterRequestDto();
            tempRegistrationDto.setEmail(otpVerificationDto.getEmail());
            // Giữ lại các thông tin khác từ DTO ban đầu nếu cần cho việc gửi lại OTP
            tempRegistrationDto.setUsername(otpVerificationDto.getUsername());
            tempRegistrationDto.setPassword(otpVerificationDto.getPassword());
            tempRegistrationDto.setConfirmPassword(otpVerificationDto.getConfirmPassword());
            tempRegistrationDto.setName(otpVerificationDto.getName());
            tempRegistrationDto.setPhoneNumber(otpVerificationDto.getPhoneNumber());

            authService.sendRegistrationOtp(tempRegistrationDto);
            redirectAttributes.addFlashAttribute("message", "Mã OTP mới đã được gửi đến email của bạn.");
            // Giữ lại các thuộc tính để hiển thị lại form verify-otp
            redirectAttributes.addFlashAttribute("otpVerificationDto", otpVerificationDto);
            return "redirect:/verify-otp";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Có lỗi xảy ra khi gửi lại OTP: " + e.getMessage());
            return "redirect:/verify-otp";
        }
    }

    @GetMapping("/home")
    public String showHomePage(Model model, HttpSession session) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        logger.info("Authentication: {}, isAuthenticated: {}",
                authentication != null ? authentication.getName() : "null",
                authentication != null && authentication.isAuthenticated());

        if (authentication != null && authentication.isAuthenticated() &&
                !authentication.getName().equals("anonymousUser")) {
            userRepository.findByUsername(authentication.getName()).ifPresent(user -> {
                String roleName = user.getRole().getName();
                LoggedUserDto loggedUserDto = new LoggedUserDto(
                        user.getUsername(),
                        roleName,
                        user.getPhoneNumber(),
                        user.getName()
                );
                session.setAttribute("loggedUser", loggedUserDto);
                model.addAttribute("currentUser", user);
                logger.info("User found: {}, Role: {}", user.getUsername(), roleName);
            });
        } else {
            session.removeAttribute("loggedUser");
            logger.info("No authenticated user or anonymous user");
        }
        return "carofix/index";
    }
}