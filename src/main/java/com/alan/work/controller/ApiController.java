package com.alan.work.controller;

import com.alan.work.entity.Literature;
import com.alan.work.entity.SearchRecord;
import com.alan.work.service.SearchService;
import com.alan.work.service.CrawlProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class ApiController {
    
    private final SearchService searchService;
    
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        log.info("API搜索请求: query={}, page={}, size={}", query, page, size);
        
        try {
            Pageable pageable = PageRequest.of(page, size);
            Page<Literature> results = searchService.searchLiterature(query, page, size);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", results.getContent());
            response.put("totalElements", results.getTotalElements());
            response.put("totalPages", results.getTotalPages());
            response.put("currentPage", page);
            response.put("size", size);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API搜索失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "搜索失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/literature/{id}")
    public ResponseEntity<Map<String, Object>> getLiterature(@PathVariable Long id) {
        log.info("API获取文献详情: id={}", id);
        
        try {
            Literature literature = searchService.getLiteratureById(id);
            
            if (literature == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "文献未找到");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", literature);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API获取文献详情失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取文献详情失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/literature/pmid/{pmid}")
    public ResponseEntity<Map<String, Object>> getLiteratureByPmid(@PathVariable String pmid) {
        log.info("API获取文献详情: pmid={}", pmid);
        
        try {
            Literature literature = searchService.getLiteratureByPmid(pmid);
            
            if (literature == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "文献未找到");
                return ResponseEntity.notFound().build();
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", literature);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API获取文献详情失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取文献详情失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/search-history")
    public ResponseEntity<Map<String, Object>> getSearchHistory() {
        log.info("API获取搜索历史");
        
        try {
            List<SearchRecord> history = searchService.getRecentSearchRecords();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", history);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API获取搜索历史失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取搜索历史失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/stats")
    public ResponseEntity<Map<String, Object>> getStats() {
        log.info("API获取统计信息");
        
        try {
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalLiterature", searchService.getAllLiterature().size());
            stats.put("totalSearches", searchService.getRecentSearchRecords().size());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", stats);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API获取统计信息失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取统计信息失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @GetMapping("/crawl-progress/{searchTerm}")
    public ResponseEntity<Map<String, Object>> getCrawlProgress(@PathVariable String searchTerm) {
        log.info("API获取爬虫进度: searchTerm={}", searchTerm);
        
        try {
            CrawlProgressService.CrawlProgress progress = searchService.getCrawlProgress(searchTerm);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", progress);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("API获取爬虫进度失败: {}", e.getMessage(), e);
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "获取爬虫进度失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}