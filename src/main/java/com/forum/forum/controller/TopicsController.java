package com.forum.forum.controller;

import com.forum.forum.controller.dto.TopicDto;
import com.forum.forum.model.Topic;
import com.forum.forum.repository.TopicRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class TopicsController {

    @Autowired
    private TopicRepository topicRepository;

    @RequestMapping("/topics")
    @ResponseBody
    public List<TopicDto> list() {
        List<Topic> topics = topicRepository.findAll();
        return TopicDto.convert(topics);
    }
}
