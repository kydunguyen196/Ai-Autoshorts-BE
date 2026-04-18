package com.autoshorts.ai.controller;

import com.autoshorts.ai.dto.FrontendBootstrapResponse;
import com.autoshorts.ai.service.FrontendMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/frontend")
public class FrontendMetadataController {

    private final FrontendMetadataService frontendMetadataService;

    public FrontendMetadataController(FrontendMetadataService frontendMetadataService) {
        this.frontendMetadataService = frontendMetadataService;
    }

    @GetMapping("/bootstrap")
    public ResponseEntity<FrontendBootstrapResponse> getBootstrapMetadata() {
        return ResponseEntity.ok(frontendMetadataService.getBootstrapMetadata());
    }
}
