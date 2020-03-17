package com.forum.forum.controller;

import com.forum.forum.controller.dto.TopicDto;
import com.forum.forum.controller.form.TopicForm;
import com.forum.forum.model.Topic;
import com.forum.forum.repository.CourseRepository;
import com.forum.forum.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/topics")
public class TopicsController {

    @Autowired
    private TopicRepository topicRepository;

    @Autowired
    private CourseRepository courseRepository;

    @GetMapping
    @ResponseBody
    public List<TopicDto> list(String courseName) {
        if(courseName == null) {
            List<Topic> topics = topicRepository.findAll();
            return TopicDto.convert(topics);
        } else {
            List<Topic> topics = topicRepository.findByCourseName(courseName);
            return TopicDto.convert(topics);
        }
    }

    @PostMapping
    public ResponseEntity<TopicDto> register(@RequestBody TopicForm form, UriComponentsBuilder uriBuilder) {
        Topic topic = form.convert(courseRepository);
        topicRepository.save(topic);

        URI uri = uriBuilder.path("/topics/{id}").buildAndExpand(topic.getId()).toUri();
        return ResponseEntity.created(uri).body(new TopicDto(topic));
    }
}
