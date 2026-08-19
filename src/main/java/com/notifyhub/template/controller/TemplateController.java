package com.notifyhub.template.controller;

import com.notifyhub.template.dto.CreateTemplateRequest;
import com.notifyhub.template.dto.TemplateResponse;
import com.notifyhub.template.service.TemplateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/templates")
public class TemplateController {

    private final TemplateService templateService;

    public TemplateController(TemplateService templateService) {
        this.templateService = templateService;
    }

    @PostMapping
    public ResponseEntity<TemplateResponse> create(
            @Valid @RequestBody CreateTemplateRequest request) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(templateService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<TemplateResponse>> findAll() {
        return ResponseEntity.ok(templateService.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<TemplateResponse> findById(
            @PathVariable Long id) {

        return ResponseEntity.ok(templateService.findById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TemplateResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody CreateTemplateRequest request) {

        return ResponseEntity.ok(
                templateService.update(id, request)
        );
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id) {

        templateService.delete(id);

        return ResponseEntity.noContent().build();
    }
}