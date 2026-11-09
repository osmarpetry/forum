package com.forum.forum.controller;

import com.forum.forum.controller.dto.DetailsOfTopicDto;
import com.forum.forum.controller.dto.TopicDto;
import com.forum.forum.controller.form.TopicForm;
import com.forum.forum.controller.form.UpdateTopicForm;
import com.forum.forum.model.Topic;
import com.forum.forum.model.User;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.Optional;

@RestController
@RequestMapping("/topics")
public class TopicsController {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    @Cacheable(value = "topicsList")
    public Page<TopicDto> list(
            @RequestParam(required = false) String courseName,
            @PageableDefault(sort = "id", direction = Sort.Direction.ASC, page = 0, size = 10) Pageable pageable) {
        if(courseName == null) {
            Page<Topic> topics = topicRepository.findAll(pageable);
            return TopicDto.convert(topics);
        } else {
            Page<Topic> topics = topicRepository.findByCourseName(courseName, pageable);
            return TopicDto.convert(topics);
        }
    }

    @PostMapping
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public ResponseEntity<TopicDto> register(@RequestBody @Valid TopicForm form, UriComponentsBuilder uriBuilder,
                                             @AuthenticationPrincipal User me) {
        Topic topic = form.convert(courseRepository);
        topic.setAuthor(me);
        topicRepository.save(topic);

        URI uri = uriBuilder.path("/topics/{id}").buildAndExpand(topic.getId()).toUri();
        return ResponseEntity.created(uri).body(new TopicDto(topic));
    }

    @GetMapping("/{id}")
    public ResponseEntity<DetailsOfTopicDto> detail(@PathVariable Long id) {
        Optional<Topic> topic = topicRepository.findById(id);
        if(topic.isPresent()) {
            return ResponseEntity.ok(new DetailsOfTopicDto(topic.get()));
        }
        return ResponseEntity.notFound().build();
    }

    @PutMapping("/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public ResponseEntity<TopicDto> update(@PathVariable Long id, @RequestBody @Valid UpdateTopicForm form,
                                           @AuthenticationPrincipal User me) {
        Optional<Topic> optional = topicRepository.findById(id);
        if(optional.isPresent()) {
            if(!isAuthor(optional.get(), me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            Topic topic = form.update(id, topicRepository);
            return ResponseEntity.ok(new TopicDto(topic));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    @CacheEvict(value = "topicsList", allEntries = true)
    public ResponseEntity<TopicDto> remove(@PathVariable Long id, @AuthenticationPrincipal User me) {
        Optional<Topic> optional = topicRepository.findById(id);
        if(optional.isPresent()) {
            if(!isAuthor(optional.get(), me)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            topicRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    private static boolean isAuthor(Topic topic, User me) {
        return topic.getAuthor() != null && topic.getAuthor().getId().equals(me.getId());
    }
}
