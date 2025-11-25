package com.alan.work.repository;

import com.alan.work.entity.Literature;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface LiteratureRepository extends JpaRepository<Literature, Long> {
    
    Optional<Literature> findByPmid(String pmid);
    
    List<Literature> findByTitleContainingIgnoreCase(String title);
    
    @Query("SELECT l FROM Literature l WHERE " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.abstractText) LIKE LOWER(CONCAT('%', :keyword, '%')) OR " +
           "LOWER(l.authors) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<Literature> searchByKeyword(@Param("keyword") String keyword);
    
    @Query("SELECT l FROM Literature l WHERE " +
           "LOWER(l.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.abstractText) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.authors) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.keywords) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Literature> findBySearchTerm(@Param("searchTerm") String searchTerm);
}