 package com.alan.work.service;

import com.alan.work.entity.Literature;
import com.alan.work.entity.SearchRecord;
import com.alan.work.repository.LiteratureRepository;
import com.alan.work.repository.SearchRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SearchService {
    
    private final LiteratureRepository literatureRepository;
    private final SearchRecordRepository searchRecordRepository;
    private final PubMedCrawlerService pubMedCrawlerService;
    private final CrawlProgressService crawlProgressService;
    
    @Transactional
    public Page<Literature> searchLiterature(String searchTerm, int page, int size) {
        log.info("搜索文献: {}, 页码: {}, 每页数量: {}", searchTerm, page, size);
        
        Pageable pageable = PageRequest.of(page, size);
        
        // 首先检查数据库中是否已有相关数据
        List<Literature> existingResults = literatureRepository.findBySearchTerm(searchTerm);
        
        if (!existingResults.isEmpty()) {
            log.info("从数据库中找到 {} 条记录", existingResults.size());
            // 手动分页
            int start = (int) pageable.getOffset();
            int end = Math.min((start + pageable.getPageSize()), existingResults.size());
            Page<Literature> pageResult = new PageImpl<>(existingResults.subList(start, end), pageable, existingResults.size());
            return pageResult;
        }
        
        // 如果数据库中没有，则进行爬取
        log.info("数据库中未找到相关记录，开始爬取数据...");
        List<Literature> crawledResults = pubMedCrawlerService.searchAndCrawl(searchTerm, 50); // 爬取前50条
        
        // 保存搜索记录
        SearchRecord searchRecord = new SearchRecord();
        searchRecord.setSearchTerm(searchTerm);
        searchRecord.setTotalResults(crawledResults.size());
        searchRecordRepository.save(searchRecord);
        
        // 返回分页结果
        List<Literature> allResults = literatureRepository.findBySearchTerm(searchTerm);
        int start = (int) pageable.getOffset();
        int end = Math.min((start + pageable.getPageSize()), allResults.size());
        return new PageImpl<>(allResults.subList(start, end), pageable, allResults.size());
    }
    
    public List<Literature> getAllLiterature() {
        return literatureRepository.findAll();
    }
    
    public Literature getLiteratureById(Long id) {
        return literatureRepository.findById(id).orElse(null);
    }
    
    public Literature getLiteratureByPmid(String pmid) {
        return literatureRepository.findByPmid(pmid).orElse(null);
    }
    
    public List<SearchRecord> getRecentSearchRecords() {
        return searchRecordRepository.findTop10ByOrderBySearchDateDesc();
    }
    
    public CrawlProgressService.CrawlProgress getCrawlProgress(String searchTerm) {
        return crawlProgressService.getProgress(searchTerm);
    }
    
    public List<SearchRecord> searchSearchRecords(String term) {
        return searchRecordRepository.findBySearchTermContainingIgnoreCaseOrderBySearchDateDesc(term);
    }
}