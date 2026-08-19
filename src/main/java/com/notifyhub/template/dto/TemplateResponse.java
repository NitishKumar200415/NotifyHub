package com.notifyhub.template.dto;

import com.notifyhub.notification.entity.NotificationChannel;

public class TemplateResponse {

    private Long id;
    private String code;
    private NotificationChannel channel;
    private String subject;
    private String bodyTemplate;

    public TemplateResponse() {
    }

    public TemplateResponse(
            Long id,
            String code,
            NotificationChannel channel,
            String subject,
            String bodyTemplate
    ) {
        this.id = id;
        this.code = code;
        this.channel = channel;
        this.subject = subject;
        this.bodyTemplate = bodyTemplate;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public NotificationChannel getChannel() {
        return channel;
    }

    public void setChannel(NotificationChannel channel) {
        this.channel = channel;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getBodyTemplate() {
        return bodyTemplate;
    }

    public void setBodyTemplate(String bodyTemplate) {
        this.bodyTemplate = bodyTemplate;
    }
}