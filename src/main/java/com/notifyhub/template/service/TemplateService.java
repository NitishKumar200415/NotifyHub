package com.notifyhub.template.service;

import com.notifyhub.notification.entity.NotificationChannel;
import com.notifyhub.template.dto.CreateTemplateRequest;
import com.notifyhub.template.dto.TemplateResponse;
import com.notifyhub.template.entity.NotificationTemplate;
import com.notifyhub.template.repository.TemplateRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TemplateService {

    private final TemplateRepository templateRepository;

    private static final Pattern PLACEHOLDER_PATTERN =
            Pattern.compile("\\{\\{\\s*([^{}]+?)\\s*\\}\\}");

    public TemplateService(TemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public TemplateResponse create(CreateTemplateRequest request) {
        NotificationTemplate template = NotificationTemplate.builder()
                .code(request.getCode())
                .channel(request.getChannel())
                .subject(request.getSubject())
                .bodyTemplate(request.getBodyTemplate())
                .build();

        return toResponse(templateRepository.save(template));
    }

    public List<TemplateResponse> findAll() {
        return templateRepository.findAll()
                .stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public TemplateResponse findById(Long id) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Template not found: " + id));

        return toResponse(template);
    }

    public TemplateResponse update(Long id, CreateTemplateRequest request) {
        NotificationTemplate template = templateRepository.findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException("Template not found: " + id));

        template.setCode(request.getCode());
        template.setChannel(request.getChannel());
        template.setSubject(request.getSubject());
        template.setBodyTemplate(request.getBodyTemplate());

        return toResponse(templateRepository.save(template));
    }

    public void delete(Long id) {
        if (!templateRepository.existsById(id)) {
            throw new IllegalArgumentException("Template not found: " + id);
        }

        templateRepository.deleteById(id);
    }

    public String render(String templateCode, Map<String, String> payload) {
        NotificationTemplate template = templateRepository.findByCode(templateCode)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Template not found: " + templateCode
                        ));

        String renderedBody = template.getBodyTemplate();

        if (payload == null || payload.isEmpty()) {
            return renderedBody;
        }

        Matcher matcher = PLACEHOLDER_PATTERN.matcher(renderedBody);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String key = matcher.group(1).trim();
            String value = payload.get(key);

            if (value == null) {
                value = matcher.group(0);
            }

            matcher.appendReplacement(
                    result,
                    Matcher.quoteReplacement(value)
            );
        }

        matcher.appendTail(result);

        return result.toString();
    }

    private TemplateResponse toResponse(NotificationTemplate template) {
        return new TemplateResponse(
                template.getId(),
                template.getCode(),
                template.getChannel(),
                template.getSubject(),
                template.getBodyTemplate()
        );
    }
}