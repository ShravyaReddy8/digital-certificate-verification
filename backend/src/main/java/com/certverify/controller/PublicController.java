package com.certverify.controller;

import com.certverify.dto.PublicStatsDto;
import com.certverify.service.PublicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
@RequiredArgsConstructor
public class PublicController {

    private final PublicService publicService;

    @GetMapping("/stats")
    public ResponseEntity<PublicStatsDto> stats() {
        return ResponseEntity.ok(publicService.stats());
    }
}
