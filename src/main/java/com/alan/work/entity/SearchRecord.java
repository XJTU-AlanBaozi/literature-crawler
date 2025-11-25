package com.alan.work.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "search_records")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SearchRecord {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "search_term", nullable = false)
    private String searchTerm;
    
    @Column(name = "total_results")
    private Integer totalResults;
    
    @Column(name = "search_date")
    private LocalDateTime searchDate;
    
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(
        name = "search_record_literature",
        joinColumns = @JoinColumn(name = "search_record_id"),
        inverseJoinColumns = @JoinColumn(name = "literature_id")
    )
    private List<Literature> results;
    
    @PrePersist
    protected void onCreate() {
        searchDate = LocalDateTime.now();
    }
}