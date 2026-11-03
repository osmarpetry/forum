package com.forum.forum.controller.ui;

import com.forum.forum.controller.form.TopicForm;
import com.forum.forum.controller.form.UpdateTopicForm;
import com.forum.forum.model.Topic;
import com.forum.forum.model.User;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class UiController {

    private static final int PAGE_SIZE = 10;

    private static final String HX_REDIRECT = "HX-Redirect";
    private static final String TOPIC_FORM = "topic-form :: form";

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;

    public UiController(TopicRepository topicRepository, CourseRepository courseRepository) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
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
        }).orElseGet(() -> notFound(response));
    }

    @GetMapping("/ui/login")
    public String login() {
        return "login";
    }

    @GetMapping("/ui/topics/new")
    public String newForm(@RequestHeader(value = "HX-Request", required = false) String htmx, Model model) {
        model.addAttribute("topicForm", new TopicForm());
        fillNewForm(model);
        return htmx != null ? TOPIC_FORM : "topic-new";
    }

    // Every UI write evicts the cache of the JSON list, so the API never shows old data.
    @PostMapping("/ui/topics")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String create(@Valid @ModelAttribute("topicForm") TopicForm form, BindingResult errors,
                         @AuthenticationPrincipal User me, Model model, HttpServletResponse response) {
        if (errors.hasErrors()) {
            fillNewForm(model);
            response.setStatus(422);
            return TOPIC_FORM;
        }
        Topic topic = form.convert(courseRepository);
        topic.setAuthor(me);
        topicRepository.save(topic);
        response.setHeader(HX_REDIRECT, "/ui/topics/" + topic.getId());
        return "layout :: empty";
    }

    @GetMapping("/ui/topics/{id}/edit")
    public String editForm(@PathVariable Long id, Model model, HttpServletResponse response) {
        return topicRepository.findById(id).map(topic -> {
            UpdateTopicForm form = new UpdateTopicForm();
            form.setTitle(topic.getTitle());
            form.setMessage(topic.getMessage());
            model.addAttribute("topicForm", form);
            fillEditForm(model, id);
            return TOPIC_FORM;
        }).orElseGet(() -> notFound(response));
    }

    @PutMapping("/ui/topics/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String update(@PathVariable Long id, @Valid @ModelAttribute("topicForm") UpdateTopicForm form,
                         BindingResult errors, Model model, HttpServletResponse response) {
        if (topicRepository.findById(id).isEmpty()) {
            return notFound(response);
        }
        if (errors.hasErrors()) {
            fillEditForm(model, id);
            response.setStatus(422);
            return TOPIC_FORM;
        }
        model.addAttribute("topic", form.update(id, topicRepository));
        return "topic :: detail";
    }

    @DeleteMapping("/ui/topics/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String delete(@PathVariable Long id, HttpServletResponse response) {
        if (topicRepository.findById(id).isEmpty()) {
            return notFound(response);
        }
        topicRepository.deleteById(id);
        response.setHeader(HX_REDIRECT, "/ui/topics");
        return "layout :: empty";
    }

    private void fillNewForm(Model model) {
        model.addAttribute("isNew", true);
        model.addAttribute("courses", courseRepository.findAll());
    }

    private void fillEditForm(Model model, Long id) {
        model.addAttribute("isNew", false);
        model.addAttribute("topicId", id);
    }

    private String notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "not-found";
    }
}
