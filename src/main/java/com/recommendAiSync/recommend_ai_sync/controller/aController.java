package com.recommendAiSync.recommend_ai_sync.controller;

import com.recommendAiSync.recommend_ai_sync.Service.SyncProductDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class aController {
    private final SyncProductDetailsService syncProductDetailsService;

    @CrossOrigin
    @GetMapping("/download-image/{categoryName}")
    public ResponseEntity downloadImage(@PathVariable String categoryName) {
        syncProductDetailsService.DownloadImageByCategory(categoryName);
        return ResponseEntity.ok().build();
    }
}
