package com.forum.forum.controller;

import com.forum.forum.controller.dto.DetailsOfTopicDto;
import com.forum.forum.controller.dto.TopicDto;
import com.forum.forum.controller.form.TopicForm;
import com.forum.forum.controller.form.UpdateTopicForm;
import com.forum.forum.model.Topic;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import javax.transaction.Transactional;
import javax.validation.Valid;
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
    public Page<TopicDto> list(@RequestParam(required = false) String courseName, @RequestParam int page, int qtd, @RequestParam String sortBy) {

        Pageable pageable = PageRequest.of(page, qtd, Sort.Direction.ASC, sortBy);

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
    public ResponseEntity<TopicDto> register(@RequestBody @Valid TopicForm form, UriComponentsBuilder uriBuilder) {
        Topic topic = form.convert(courseRepository);
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
    public ResponseEntity<TopicDto> update(@PathVariable Long id, @RequestBody @Valid UpdateTopicForm form) {
        Optional<Topic> optional = topicRepository.findById(id);
        if(optional.isPresent()) {
            Topic topic = form.update(id, topicRepository);
            return ResponseEntity.ok(new TopicDto(topic));
        }
        return ResponseEntity.notFound().build();
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<TopicDto> remove(@PathVariable Long id) {
        Optional<Topic> optional = topicRepository.findById(id);
        if(optional.isPresent()) {
            topicRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
