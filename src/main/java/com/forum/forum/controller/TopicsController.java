package com.forum.forum.controller;

import com.forum.forum.model.Course;
import com.forum.forum.model.Topic;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Arrays;
import java.util.List;

@Controller
public class TopicsController {

    @RequestMapping("/topics")
    @ResponseBody
    public List<Topic> list() {
        Topic topic = new Topic("Doubt", "Spring Doubt", new Course("Spring", "Programming"));

        return Arrays.asList(topic, topic, topic);
    }
}
