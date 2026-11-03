package com.forum.forum.controller.ui;

import com.forum.forum.controller.form.SignupForm;
import com.forum.forum.model.User;
import com.forum.forum.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class SignupController {

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
        if (!errors.hasFieldErrors("email") && userRepository.findByEmail(form.getEmail()).isPresent()) {
            errors.rejectValue("email", "email.taken", "Email is already registered");
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
