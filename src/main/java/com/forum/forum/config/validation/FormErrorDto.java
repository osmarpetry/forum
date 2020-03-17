package com.forum.forum.config.validation;

public class FormErrorDto {

    private String slot;
    private String error;

    public FormErrorDto(String slot, String error) {
        this.slot = slot;
        this.error = error;
    }

    public String getSlot() {
        return slot;
    }

    public String getError() {
        return error;
    }
}
