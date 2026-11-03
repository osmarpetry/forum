package com.forum.forum.controller.ui;

import com.forum.forum.model.Topic;
import com.forum.forum.repository.TopicRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UiController {

    private static final int PAGE_SIZE = 10;

    private final TopicRepository topicRepository;

    public UiController(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @GetMapping("/")
    public String root() {
        return "redirect:/ui/topics";
    }

    // One URL, two answers: a full page, or only the "list" fragment when htmx asks.
    @GetMapping("/ui/topics")
    public String list(@RequestParam(required = false) String courseName,
                       @RequestParam(defaultValue = "0") int page,
                       @RequestHeader(value = "HX-Request", required = false) String htmx,
                       Model model) {
        PageRequest pageable = PageRequest.of(Math.max(page, 0), PAGE_SIZE, Sort.by("id"));
        boolean filtered = courseName != null && !courseName.isBlank();
        Page<Topic> topics = filtered
                ? topicRepository.findByCourseName(courseName, pageable)
                : topicRepository.findAll(pageable);
        model.addAttribute("page", topics);
        model.addAttribute("courseName", filtered ? courseName : "");
        return htmx != null ? "topics :: list" : "topics";
    }

    @GetMapping("/ui/topics/{id}")
    public String detail(@PathVariable Long id, Model model, HttpServletResponse response) {
        return topicRepository.findById(id).map(topic -> {
            model.addAttribute("topic", topic);
            return "topic";
        }).orElseGet(() -> {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return "not-found";
        });
    }

    @GetMapping("/ui/login")
    public String login() {
        return "login";
    }
}
