package com.alan.work.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

@Entity
@Table(name = "literatures")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Literature {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true)
    private String pmid;
    
    @Column(length = 1000)
    private String title;
    
    @Column(length = 3000)
    private String authors;
    
    @Column(length = 5000)
    private String abstractText;
    
    private LocalDate publicationDate;
    
    private String journal;
    
    @Column(length = 2000)
    private String affiliation;
    
    @Column(length = 1000)
    private String keywords;
    
    private String doi;
    
    @Column(length = 2000)
    private String url;
    
    @Column(length = 1000)
    private String publicationTypes;
    
    @Column(length = 2000)
    private String meshTerms;
    
    @Lob
    @Column(columnDefinition = "TEXT")
    private String fullText;
    
    @ElementCollection
    @CollectionTable(name = "literature_authors", joinColumns = @JoinColumn(name = "literature_id"))
    @Column(name = "author")
    private List<String> authorList;
    
    private LocalDate crawledDate;
    
    @PrePersist
    protected void onCreate() {
        crawledDate = LocalDate.now();
    }
}