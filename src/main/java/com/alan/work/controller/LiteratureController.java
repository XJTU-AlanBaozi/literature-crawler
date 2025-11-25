package com.alan.work.controller;

import com.alan.work.entity.Literature;
import com.alan.work.entity.SearchRecord;
import com.alan.work.service.SearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class LiteratureController {
    
    private final SearchService searchService;
    
    @GetMapping("/")
    public String index(Model model) {
        List<SearchRecord> recentSearches = searchService.getRecentSearchRecords();
        model.addAttribute("recentSearches", recentSearches);
        return "index";
    }
    
    @GetMapping("/search")
    public String search(@RequestParam("query") String query,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        
        log.info("搜索请求: query={}, page={}, size={}", query, page, size);
        
        if (query == null || query.trim().isEmpty()) {
            model.addAttribute("error", "请输入搜索关键词");
            return "index";
        }
        
        try {
            Page<Literature> results = searchService.searchLiterature(query.trim(), page, size);
            
            model.addAttribute("results", results);
            model.addAttribute("query", query);
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", results.getTotalPages());
            model.addAttribute("totalResults", results.getTotalElements());
            
            return "search-results";
            
        } catch (Exception e) {
            log.error("搜索失败: {}", e.getMessage(), e);
            model.addAttribute("error", "搜索失败，请稍后重试");
            return "index";
        }
    }
    
    @GetMapping("/search-history")
    public String searchHistory(Model model) {
        List<SearchRecord> recentSearches = searchService.getRecentSearchRecords();
        model.addAttribute("recentSearches", recentSearches);
        return "search-history";
    }
    
    @GetMapping("/literature/{id}")
    public String literatureDetail(@PathVariable Long id, Model model) {
        log.info("获取文献详情页面: id={}", id);
        
        try {
            Literature literature = searchService.getLiteratureById(id);
            if (literature == null) {
                model.addAttribute("error", "文献未找到");
                return "error";
            }
            
            model.addAttribute("literature", literature);
            return "literature-detail";
            
        } catch (Exception e) {
            log.error("获取文献详情失败: {}", e.getMessage(), e);
            model.addAttribute("error", "获取文献详情失败: " + e.getMessage());
            return "error";
        }
    }
}