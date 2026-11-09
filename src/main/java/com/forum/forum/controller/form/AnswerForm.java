package com.forum.forum.controller.form;

import com.forum.forum.model.Answer;
import com.forum.forum.model.Topic;
import com.forum.forum.model.User;
import jakarta.validation.constraints.NotNull;
import org.hibernate.validator.constraints.Length;

public class AnswerForm {

    private static final String TOO_SHORT = "Message must have at least 10 characters";

    @NotNull(message = TOO_SHORT) @Length(min = 10, message = TOO_SHORT)
    private String message;

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Answer convert(Topic topic, User author) {
        Answer answer = new Answer();
        answer.setMessage(message);
        answer.setTopic(topic);
        answer.setAuthor(author);
        return answer;
    }
}
