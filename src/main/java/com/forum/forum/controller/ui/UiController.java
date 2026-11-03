package com.forum.forum.controller.ui;

import com.forum.forum.controller.form.AnswerForm;
import com.forum.forum.controller.form.TopicForm;
import com.forum.forum.controller.form.UpdateTopicForm;
import com.forum.forum.model.Answer;
import com.forum.forum.model.Topic;
import com.forum.forum.model.TopicStatus;
import com.forum.forum.model.User;
import com.forum.forum.repository.AnswerRepository;
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
    private static final String ANSWERS = "topic :: answers";

    private final TopicRepository topicRepository;
    private final CourseRepository courseRepository;
    private final AnswerRepository answerRepository;

    public UiController(TopicRepository topicRepository, CourseRepository courseRepository,
                        AnswerRepository answerRepository) {
        this.topicRepository = topicRepository;
        this.courseRepository = courseRepository;
        this.answerRepository = answerRepository;
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
    public String detail(@PathVariable Long id, @AuthenticationPrincipal User me, Model model,
                         HttpServletResponse response) {
        return topicRepository.findById(id).map(topic -> {
            model.addAttribute("topic", topic);
            model.addAttribute("owner", isOwner(topic, me));
            model.addAttribute("answerForm", new AnswerForm());
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
    public String editForm(@PathVariable Long id, @AuthenticationPrincipal User me, Model model,
                           HttpServletResponse response) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return notFound(response);
        }
        if (!isOwner(topic, me)) {
            return forbidden(response);
        }
        UpdateTopicForm form = new UpdateTopicForm();
        form.setTitle(topic.getTitle());
        form.setMessage(topic.getMessage());
        model.addAttribute("topicForm", form);
        fillEditForm(model, id);
        return TOPIC_FORM;
    }

    @PutMapping("/ui/topics/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String update(@PathVariable Long id, @Valid @ModelAttribute("topicForm") UpdateTopicForm form,
                         BindingResult errors, @AuthenticationPrincipal User me, Model model,
                         HttpServletResponse response) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return notFound(response);
        }
        if (!isOwner(topic, me)) {
            return forbidden(response);
        }
        if (errors.hasErrors()) {
            fillEditForm(model, id);
            response.setStatus(422);
            return TOPIC_FORM;
        }
        model.addAttribute("topic", form.update(id, topicRepository));
        model.addAttribute("owner", true);
        return "topic :: detail";
    }

    @DeleteMapping("/ui/topics/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String delete(@PathVariable Long id, @AuthenticationPrincipal User me, HttpServletResponse response) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return notFound(response);
        }
        if (!isOwner(topic, me)) {
            return forbidden(response);
        }
        topicRepository.delete(topic);
        response.setHeader(HX_REDIRECT, "/ui/topics");
        return "layout :: empty";
    }

    @PostMapping("/ui/topics/{id}/answers")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String reply(@PathVariable Long id, @Valid @ModelAttribute("answerForm") AnswerForm form,
                        BindingResult errors, @AuthenticationPrincipal User me, Model model,
                        HttpServletResponse response) {
        Topic topic = topicRepository.findById(id).orElse(null);
        if (topic == null) {
            return notFound(response);
        }
        model.addAttribute("topic", topic);
        model.addAttribute("owner", isOwner(topic, me));
        if (errors.hasErrors()) {
            response.setStatus(422);
            return ANSWERS;
        }
        Answer answer = answerRepository.save(form.convert(topic, me));
        topic.getAnswers().add(answer);
        if (topic.getStatus() == TopicStatus.NOT_ANSWERED) {
            topic.setStatus(TopicStatus.NOT_RESOLVED);
        }
        form.setMessage("");
        model.addAttribute("oob", true);
        return ANSWERS;
    }

    @PostMapping("/ui/answers/{id}/solution")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public String markSolution(@PathVariable Long id, @AuthenticationPrincipal User me, Model model,
                               HttpServletResponse response) {
        Answer answer = answerRepository.findById(id).orElse(null);
        if (answer == null) {
            return notFound(response);
        }
        Topic topic = answer.getTopic();
        if (!isOwner(topic, me)) {
            return forbidden(response);
        }
        topic.getAnswers().forEach(a -> a.setSolution(a.getId().equals(id)));
        topic.setStatus(TopicStatus.RESOLVED);
        model.addAttribute("topic", topic);
        model.addAttribute("owner", true);
        model.addAttribute("answerForm", new AnswerForm());
        model.addAttribute("oob", true);
        return ANSWERS;
    }

    // Compare by id: the logged-in user is a detached copy, and the author can be a Hibernate proxy.
    // A topic made through the JSON API can have no author: then nobody owns it.
    private boolean isOwner(Topic topic, User me) {
        return me != null && topic.getAuthor() != null && topic.getAuthor().getId().equals(me.getId());
    }

    private void fillNewForm(Model model) {
        model.addAttribute("isNew", true);
        model.addAttribute("courses", courseRepository.findAll());
    }

    private void fillEditForm(Model model, Long id) {
        model.addAttribute("isNew", false);
        model.addAttribute("topicId", id);
    }

    private String forbidden(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        return "forbidden";
    }

    private String notFound(HttpServletResponse response) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        return "not-found";
    }
}
