package com.forum.forum.controller.ui;

import com.forum.forum.controller.form.SignupForm;
import com.forum.forum.model.User;
import com.forum.forum.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignupController {

    private static final int MAX_PASSWORD_BYTES = 72;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SignupController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping("/ui/signup")
    public String form(Model model) {
        model.addAttribute("signupForm", new SignupForm());
        return "signup";
    }

    @PostMapping("/ui/signup")
    public String signup(@Valid @ModelAttribute("signupForm") SignupForm form, BindingResult errors,
                         HttpServletResponse response) {
        if (form.getEmail() != null) {
            form.setEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
        }
        if (!errors.hasFieldErrors("email") && userRepository.existsByEmailIgnoreCase(form.getEmail())) {
            errors.rejectValue("email", "email.taken", "Email is already registered");
        }
        if (!errors.hasFieldErrors("password")
                && form.getPassword().getBytes(StandardCharsets.UTF_8).length > MAX_PASSWORD_BYTES) {
            errors.rejectValue("password", "password.tooLong", "Password must have at most 72 bytes");
        }
        if (errors.hasErrors()) {
            response.setStatus(422);
            return "signup";
        }
        User user = new User();
        user.setName(form.getName());
        user.setEmail(form.getEmail());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        userRepository.save(user);
        return "redirect:/ui/login?registered";
    }
}
