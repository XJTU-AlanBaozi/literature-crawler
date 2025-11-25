package com.alan.work.service;

import com.alan.work.entity.Literature;
import com.alan.work.repository.LiteratureRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class PubMedCrawlerService {
    
    private final LiteratureRepository literatureRepository;
    private final CrawlProgressService crawlProgressService;
    
    @Autowired(required = false)
    private WebDriver webDriver;
    
    @Autowired
    public PubMedCrawlerService(LiteratureRepository literatureRepository, CrawlProgressService crawlProgressService) {
        this.literatureRepository = literatureRepository;
        this.crawlProgressService = crawlProgressService;
    }
    
    private static final String PUBMED_BASE_URL = "https://pubmed.ncbi.nlm.nih.gov";
    private static final int TIMEOUT_SECONDS = 30;
    
    public List<Literature> searchAndCrawl(String searchTerm, int maxResults) {
        List<Literature> results = new ArrayList<>();
        
        try {
            // 开始进度跟踪
            crawlProgressService.startCrawl(searchTerm, maxResults);
            
            // 第一步：搜索获取文献列表
            String searchUrl = PUBMED_BASE_URL + "/?term=" + searchTerm + "&size=" + maxResults;
            log.info("开始搜索: {}", searchUrl);
            crawlProgressService.updateProgress(searchTerm, 0, "正在搜索文献...");
            
            Document searchDoc = Jsoup.connect(searchUrl)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(TIMEOUT_SECONDS * 1000)
                    .get();
            
            // 提取搜索结果
            Elements articleElements = searchDoc.select(".docsum-content");
            log.info("找到 {} 个搜索结果", articleElements.size());
            
            int processedCount = 0;
            for (Element articleElement : articleElements) {
                try {
                    Literature literature = extractBasicInfo(articleElement);
                    if (literature != null) {
                        results.add(literature);
                        processedCount++;
                        crawlProgressService.updateProgress(searchTerm, processedCount, 
                            String.format("正在提取基本信息 (%d/%d)...", processedCount, articleElements.size()));
                    }
                } catch (Exception e) {
                    log.error("提取文献基本信息失败: {}", e.getMessage());
                }
            }
            
            // 第二步：使用Selenium获取详细信息
            if (!results.isEmpty() && webDriver != null) {
                crawlProgressService.updateProgress(searchTerm, processedCount, 
                    String.format("正在获取详细信息 (%d/%d)...", 0, results.size()));
                    
                for (int i = 0; i < results.size(); i++) {
                    Literature literature = results.get(i);
                    try {
                        extractDetailedInfo(literature);
                        crawlProgressService.updateProgress(searchTerm, processedCount, 
                            String.format("正在获取详细信息 (%d/%d)...", i + 1, results.size()));
                        Thread.sleep(1000); // 避免请求过快
                    } catch (Exception e) {
                        log.error("提取文献详细信息失败: {}", e.getMessage());
                    }
                }
            }
            
            // 保存到数据库
            if (!results.isEmpty()) {
                crawlProgressService.updateProgress(searchTerm, processedCount, "正在保存数据...");
                literatureRepository.saveAll(results);
            }
            
            crawlProgressService.completeCrawl(searchTerm);
            log.info("成功爬取并保存 {} 条文献", results.size());
            
        } catch (Exception e) {
            log.error("爬取过程失败: {}", e.getMessage(), e);
            crawlProgressService.failCrawl(searchTerm, e.getMessage());
        }
        
        return results;
    }
    
    private Literature extractBasicInfo(Element articleElement) {
        try {
            Literature literature = new Literature();
            
            // 提取PMID
            Element pmidElement = articleElement.selectFirst(".docsum-pmid");
            if (pmidElement != null) {
                literature.setPmid(pmidElement.text().trim());
            }
            
            // 提取标题
            Element titleElement = articleElement.selectFirst(".docsum-title");
            if (titleElement != null) {
                literature.setTitle(titleElement.text().trim());
            }
            
            // 提取作者
            Element authorsElement = articleElement.selectFirst(".docsum-authors");
            if (authorsElement != null) {
                literature.setAuthors(authorsElement.text().trim());
            }
            
            // 提取发表日期
            Element dateElement = articleElement.selectFirst(".docsum-journal-citation");
            if (dateElement != null) {
                String dateText = dateElement.text().trim();
                LocalDate pubDate = parseDate(dateText);
                if (pubDate != null) {
                    literature.setPublicationDate(pubDate);
                }
            }
            
            // 提取摘要
            Element abstractElement = articleElement.selectFirst(".docsum-snippet");
            if (abstractElement != null) {
                literature.setAbstractText(abstractElement.text().trim());
            }
            
            // 构建详情页URL
            if (literature.getPmid() != null) {
                literature.setUrl(PUBMED_BASE_URL + "/" + literature.getPmid());
            }
            
            return literature;
            
        } catch (Exception e) {
            log.error("提取基本信息失败: {}", e.getMessage());
            return null;
        }
    }
    
    private void extractDetailedInfo(Literature literature) {
        // 如果没有WebDriver，只使用基本信息
        if (webDriver == null) {
            log.warn("WebDriver不可用，仅使用基本信息: {}", literature.getTitle());
            return;
        }
        
        WebDriver driver = null;
        try {
            // 如果webDriver是ChromeDriver，使用它；否则创建新的实例
            if (webDriver instanceof ChromeDriver) {
                driver = webDriver;
            } else {
                // 设置Chrome选项
                ChromeOptions options = new ChromeOptions();
                options.addArguments("--headless"); // 无头模式
                options.addArguments("--no-sandbox");
                options.addArguments("--disable-dev-shm-usage");
                options.addArguments("--disable-gpu");
                options.addArguments("--window-size=1920,1080");
                
                driver = new ChromeDriver(options);
            }
            
            log.info("访问详情页: {}", literature.getUrl());
            driver.get(literature.getUrl());
            
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(TIMEOUT_SECONDS));
            
            // 等待页面加载完成
            wait.until(ExpectedConditions.presenceOfElementLocated(By.cssSelector(".article-details")));
            
            // 提取详细信息
            extractAffiliationInfo(driver, literature);
            extractJournalInfo(driver, literature);
            extractDOI(driver, literature);
            extractKeywords(driver, literature);
            extractMeshTerms(driver, literature);
            
            log.info("成功提取详细信息: {}", literature.getTitle());
            
        } catch (Exception e) {
            log.error("提取详细信息失败: {}", e.getMessage());
        } finally {
            // 只关闭我们创建的driver，不关闭注入的driver
            if (driver != null && driver != webDriver) {
                driver.quit();
            }
        }
    }
    
    private void extractAffiliationInfo(WebDriver driver, Literature literature) {
        try {
            // 使用XPath定位作者信息
            List<WebElement> affiliationElements = driver.findElements(By.xpath("//div[@class='affiliation-links']//a"));
            if (!affiliationElements.isEmpty()) {
                StringBuilder affiliations = new StringBuilder();
                for (WebElement element : affiliationElements) {
                    affiliations.append(element.getText()).append("; ");
                }
                literature.setAffiliation(affiliations.toString().trim());
            }
        } catch (Exception e) {
            log.warn("提取affiliation信息失败: {}", e.getMessage());
        }
    }
    
    private void extractJournalInfo(WebDriver driver, Literature literature) {
        try {
            WebElement journalElement = driver.findElement(By.cssSelector(".journal-actions"));
            if (journalElement != null) {
                literature.setJournal(journalElement.getText().trim());
            }
        } catch (Exception e) {
            log.warn("提取期刊信息失败: {}", e.getMessage());
        }
    }
    
    private void extractDOI(WebDriver driver, Literature literature) {
        try {
            WebElement doiElement = driver.findElement(By.cssSelector(".identifier.doi"));
            if (doiElement != null) {
                literature.setDoi(doiElement.getText().trim());
            }
        } catch (Exception e) {
            log.warn("提取DOI失败: {}", e.getMessage());
        }
    }
    
    private void extractKeywords(WebDriver driver, Literature literature) {
        try {
            List<WebElement> keywordElements = driver.findElements(By.cssSelector(".keywords-list li"));
            if (!keywordElements.isEmpty()) {
                StringBuilder keywords = new StringBuilder();
                for (WebElement element : keywordElements) {
                    keywords.append(element.getText()).append(", ");
                }
                literature.setKeywords(keywords.toString().trim());
            }
        } catch (Exception e) {
            log.warn("提取关键词失败: {}", e.getMessage());
        }
    }
    
    private void extractMeshTerms(WebDriver driver, Literature literature) {
        try {
            List<WebElement> meshElements = driver.findElements(By.cssSelector(".mesh-terms li"));
            if (!meshElements.isEmpty()) {
                StringBuilder meshTerms = new StringBuilder();
                for (WebElement element : meshElements) {
                    meshTerms.append(element.getText()).append(", ");
                }
                literature.setMeshTerms(meshTerms.toString().trim());
            }
        } catch (Exception e) {
            log.warn("提取MeSH术语失败: {}", e.getMessage());
        }
    }
    
    private LocalDate parseDate(String dateText) {
        try {
            if (dateText == null || dateText.trim().isEmpty()) {
                return null;
            }
            
            // 清理日期文本，提取年份信息
            String cleanText = dateText.trim();
            
            // 尝试从文本中提取年份和月份信息
            // PubMed常见的格式如: "2023 Nov 15", "2023 Nov", "Nov 2023", "2023"
            String[] patterns = {
                "yyyy MMM dd",     // 2023 Nov 15
                "yyyy MMM",        // 2023 Nov  
                "MMM yyyy",        // Nov 2023
                "yyyy",            // 2023
                "MMM dd yyyy"      // Nov 15 2023
            };
            
            for (String pattern : patterns) {
                try {
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(pattern);
                    return LocalDate.parse(cleanText, formatter);
                } catch (Exception e) {
                    // 尝试下一个格式
                }
            }
            
            // 如果标准格式解析失败，尝试提取年份信息
            java.util.regex.Pattern yearPattern = java.util.regex.Pattern.compile("\\d{4}");
            java.util.regex.Matcher matcher = yearPattern.matcher(cleanText);
            if (matcher.find()) {
                int year = Integer.parseInt(matcher.group());
                // 如果只有年份，设置为1月1日
                return LocalDate.of(year, 1, 1);
            }
            
            log.warn("无法解析日期文本: {}", dateText);
        } catch (Exception e) {
            log.warn("解析日期失败: {}, 错误: {}", dateText, e.getMessage());
        }
        return null;
    }
}