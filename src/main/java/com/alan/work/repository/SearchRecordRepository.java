package com.alan.work.repository;

import com.alan.work.entity.SearchRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SearchRecordRepository extends JpaRepository<SearchRecord, Long> {
    
    List<SearchRecord> findBySearchTermContainingIgnoreCaseOrderBySearchDateDesc(String searchTerm);
    
    List<SearchRecord> findTop10ByOrderBySearchDateDesc();
}