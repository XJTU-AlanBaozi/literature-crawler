package com.alan.work.service;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class CrawlProgressService {
    
    private final Map<String, CrawlProgress> progressMap = new ConcurrentHashMap<>();
    
    public void startCrawl(String searchTerm, int totalItems) {
        CrawlProgress progress = new CrawlProgress();
        progress.setSearchTerm(searchTerm);
        progress.setTotalItems(totalItems);
        progress.setProcessedItems(0);
        progress.setStatus("开始爬取...");
        progress.setPercentage(0);
        progress.setActive(true);
        
        progressMap.put(searchTerm, progress);
        log.info("开始爬取进度跟踪: {}, 总数: {}", searchTerm, totalItems);
    }
    
    public void updateProgress(String searchTerm, int processedItems, String status) {
        CrawlProgress progress = progressMap.get(searchTerm);
        if (progress != null) {
            progress.setProcessedItems(processedItems);
            progress.setStatus(status);
            
            int percentage = 0;
            if (progress.getTotalItems() > 0) {
                percentage = (processedItems * 100) / progress.getTotalItems();
            }
            progress.setPercentage(Math.min(percentage, 100));
            
            log.debug("更新进度: {} - {}/{} ({}%)", searchTerm, processedItems, progress.getTotalItems(), percentage);
        }
    }
    
    public void completeCrawl(String searchTerm) {
        CrawlProgress progress = progressMap.get(searchTerm);
        if (progress != null) {
            progress.setStatus("爬取完成");
            progress.setPercentage(100);
            progress.setProcessedItems(progress.getTotalItems());
            progress.setActive(false);
            
            log.info("爬取完成: {}", searchTerm);
        }
    }
    
    public void failCrawl(String searchTerm, String error) {
        CrawlProgress progress = progressMap.get(searchTerm);
        if (progress != null) {
            progress.setStatus("爬取失败: " + error);
            progress.setActive(false);
            
            log.error("爬取失败: {}, 错误: {}", searchTerm, error);
        }
    }
    
    public CrawlProgress getProgress(String searchTerm) {
        return progressMap.get(searchTerm);
    }
    
    public void removeProgress(String searchTerm) {
        progressMap.remove(searchTerm);
        log.info("移除进度跟踪: {}", searchTerm);
    }
    
    @Data
    public static class CrawlProgress {
        private String searchTerm;
        private int totalItems;
        private int processedItems;
        private int percentage;
        private String status;
        private boolean isActive;
        private long startTime = System.currentTimeMillis();
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
        
        public String getFormattedElapsedTime() {
            long elapsed = getElapsedTime();
            long seconds = elapsed / 1000;
            long minutes = seconds / 60;
            long hours = minutes / 60;
            
            if (hours > 0) {
                return String.format("%d小时%d分%d秒", hours, minutes % 60, seconds % 60);
            } else if (minutes > 0) {
                return String.format("%d分%d秒", minutes, seconds % 60);
            } else {
                return String.format("%d秒", seconds);
            }
        }
    }
}