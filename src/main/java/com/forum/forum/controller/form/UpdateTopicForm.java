package com.forum.forum.controller.form;

import com.forum.forum.model.Topic;
import com.forum.forum.repository.TopicRepository;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

public class UpdateTopicForm {

    @NotNull @NotEmpty @Length(min = 5)
    private String title;
    @NotNull @NotEmpty @Length(min = 10)
    private String message;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Topic update(Long id, TopicRepository topicRepository) {
        Topic topic = topicRepository.getOne(id);

        topic.setTitle(this.title);
        topic.setMessage(this.message);

        return topic;
    }
}
