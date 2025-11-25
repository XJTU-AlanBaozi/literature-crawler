# AI使用记录

## 项目概述

本项目是一个基于AI辅助开发的文献爬虫系统，能够自动从PubMed网站爬取医学文献信息，并提供Web界面进行搜索、展示和分析。在开发过程中，AI工具为项目架构设计、技术选型、代码实现和问题解决方案提供了重要支持。

## AI使用场景

### 1. 架构设计阶段

#### 技术选型
- **后端框架**: AI建议使用Spring Boot 3.5.8，因其稳定性好、生态丰富
- **数据库**: AI推荐使用H2内存数据库，便于开发和测试
- **爬虫技术**: AI建议使用Selenium + Jsoup组合，兼顾效率和完整性
- **前端框架**: AI推荐Bootstrap 5，提供现代化的UI组件

#### 架构模式
- **分层架构**: Controller → Service → Repository
- **实体设计**: Literature、Author、SearchRecord等核心实体
- **服务拆分**: 爬虫服务、搜索服务、统计服务分离

### 2. 爬虫实现阶段

#### 爬取策略设计
AI建议采用两步爬取策略：
1. **第一步**: 使用Jsoup快速获取列表页基本信息（标题、作者、摘要等）
2. **第二步**: 使用Selenium获取详情页完整信息（机构、DOI、全文等）

#### 反爬虫对策
- **请求头伪装**: 设置User-Agent、Accept等请求头
- **随机延迟**: 添加随机等待时间，避免被检测
- **异常处理**: 完善的异常捕获和重试机制

#### 代码实现
```java
// AI生成的爬虫核心代码结构
public class PubMedCrawlerService {
    public Literature crawlLiterature(String pmid) {
        // 第一步：获取基本信息
        Literature basicInfo = crawlBasicInfo(pmid);
        
        // 第二步：获取详细信息
        Literature detailInfo = crawlDetailInfo(pmid);
        
        // 合并信息
        return mergeLiteratureInfo(basicInfo, detailInfo);
    }
}
```

### 3. 前端界面设计

#### 页面布局
- **响应式设计**: AI建议使用Bootstrap的网格系统
- **搜索界面**: 简洁的搜索框和按钮设计
- **结果展示**: 卡片式布局，清晰展示文献信息
- **详情弹窗**: 模态框展示完整的文献详情

#### 用户体验优化
- **加载动画**: 搜索过程中的进度提示
- **分页功能**: 支持大量结果的分页浏览
- **搜索历史**: 记录用户的搜索行为
- **错误提示**: 友好的错误信息展示

### 3. 问题解决方案

#### 数据库连接问题
**问题**: H2数据库连接失败
**AI解决方案**: 
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password: 
```

#### 爬虫超时问题
**问题**: 爬虫请求超时，导致数据获取失败
**AI解决方案**: 
```java
// 设置合理的超时时间
ChromeOptions options = new ChromeOptions();
options.addArguments("--no-sandbox");
options.addArguments("--disable-dev-shm-usage");

// 设置页面加载超时
webDriver.manage().timeouts().pageLoadTimeout(30, TimeUnit.SECONDS);
webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);
```

#### 前端样式问题
**问题**: 模态框样式不符合预期
**AI解决方案**: 
```css
.detail-modal .modal-body {
    max-height: 70vh;
    overflow-y: auto;
}

.detail-section {
    margin-bottom: 1.5rem;
    padding: 1rem;
    background: #f8f9fa;
    border-radius: 0.5rem;
}
```

## 核心技术实现

### 1. 爬虫服务实现

#### 基本信息爬取
```java
private Literature crawlBasicInfo(String pmid) {
    String url = PUBMED_BASE_URL + pmid;
    Document doc = Jsoup.connect(url)
        .userAgent(USER_AGENT)
        .timeout(30000)
        .get();
    
    Literature literature = new Literature();
    literature.setPmid(pmid);
    literature.setTitle(extractTitle(doc));
    literature.setAuthors(extractAuthors(doc));
    literature.setAbstractText(extractAbstract(doc));
    
    return literature;
}
```

#### 详细信息爬取
```java
private Literature crawlDetailInfo(String pmid) {
    WebDriver webDriver = createWebDriver();
    try {
        String url = PUBMED_BASE_URL + pmid;
        webDriver.get(url);
        
        // 等待页面加载完成
        WebDriverWait wait = new WebDriverWait(webDriver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".full-text-links-list")
        ));
        
        // 获取页面源代码
        String pageSource = webDriver.getPageSource();
        Document doc = Jsoup.parse(pageSource);
        
        Literature literature = new Literature();
        literature.setDoi(extractDOI(doc));
        literature.setInstitution(extractInstitution(doc));
        literature.setFullTextLinks(extractFullTextLinks(doc));
        
        return literature;
    } finally {
        webDriver.quit();
    }
}
```

### 2. 搜索服务实现

#### 智能搜索策略
```java
public Page<Literature> searchLiterature(String query, Pageable pageable) {
    // 1. 先搜索数据库
    Page<Literature> dbResults = literatureRepository
        .searchByKeyword(query, pageable);
    
    // 2. 如果数据库中没有，再爬取
    if (dbResults.isEmpty()) {
        List<Literature> crawledLiterature = pubMedCrawlerService
            .searchAndCrawlLiterature(query);
        
        // 保存爬取结果
        literatureRepository.saveAll(crawledLiterature);
        
        // 重新搜索数据库
        dbResults = literatureRepository
            .searchByKeyword(query, pageable);
    }
    
    return dbResults;
}
```

#### 搜索历史记录
```java
public void saveSearchRecord(String query, int resultCount) {
    SearchRecord record = new SearchRecord();
    record.setQuery(query);
    record.setResultCount(resultCount);
    record.setSearchTime(LocalDateTime.now());
    
    searchRecordRepository.save(record);
}
```

### 3. 控制器层实现

#### 搜索控制器
```java
@Controller
public class SearchController {
    
    @GetMapping("/search")
    public String search(@RequestParam String query,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size,
                        Model model) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Literature> results = searchService.searchLiterature(query, pageable);
        
        model.addAttribute("query", query);
        model.addAttribute("results", results);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", results.getTotalPages());
        
        // 保存搜索历史
        searchService.saveSearchRecord(query, results.getNumberOfElements());
        
        return "search-results";
    }
}
```

#### 详情控制器
```java
@GetMapping("/literature/{id}")
@ResponseBody
public Literature getLiteratureDetail(@PathVariable Long id) {
    return literatureRepository.findById(id)
        .orElseThrow(() -> new ResponseStatusException(
            HttpStatus.NOT_FOUND, "文献不存在"
        ));
}
```

## 问题解决方案

### 1. 爬虫稳定性问题

#### 问题描述
爬虫在运行过程中经常出现超时、连接失败等问题

#### AI解决方案
1. **添加重试机制**: 失败后自动重试3次
2. **设置合理超时**: 页面加载超时30秒，元素等待10秒
3. **异常处理**: 捕获所有异常，记录日志并返回友好错误
4. **资源清理**: 确保WebDriver正确关闭

```java
@Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000))
public Literature crawlLiterature(String pmid) {
    // 爬虫逻辑
}
```

### 2. 前端性能问题

#### 问题描述
搜索结果页面加载缓慢，用户体验不佳

#### AI解决方案
1. **分页加载**: 每次只加载10条结果
2. **异步加载**: 详情信息异步加载，不阻塞页面
3. **缓存机制**: 已查看的详情信息本地缓存
4. **加载动画**: 提供视觉反馈，提升用户体验

### 3. 数据一致性问题

#### 问题描述
同一篇文献在不同时间爬取的信息不一致

#### AI解决方案
1. **PMID作为主键**: 使用PMID确保文献唯一性
2. **增量更新**: 只更新变化的字段
3. **时间戳记录**: 记录爬取时间，便于追踪
4. **数据验证**: 对关键字段进行格式验证

## 部署说明

### 开发环境
```bash
# 1. 克隆项目
git clone <项目地址>
cd literature-crawler

# 2. 配置ChromeDriver
# 下载对应版本的ChromeDriver
# 配置WebDriverConfig.java中的路径

# 3. 运行项目
mvn spring-boot:run

# 4. 访问系统
http://localhost:8080
```

### 生产环境
```bash
# 1. 打包项目
mvn clean package

# 2. 运行jar包
java -jar target/work-0.0.1-SNAPSHOT.jar

# 3. 配置环境变量
export DATABASE_URL="jdbc:mysql://localhost:3306/literature"
export CHROMEDRIVER_PATH="/usr/local/bin/chromedriver"
```

## 使用说明

### 基本搜索
1. 在首页搜索框输入关键词（如：COVID-19, cancer research）
2. 点击"搜索文献"按钮
3. 查看搜索结果列表
4. 点击文献标题查看详细信息

### 高级功能
- **分页浏览**: 使用分页控件浏览更多结果
- **搜索历史**: 查看最近的搜索记录
- **详情查看**: 在模态框中查看完整的文献信息
- **重新搜索**: 点击搜索历史重新执行搜索

### API接口
```bash
# 搜索文献
curl "http://localhost:8080/api/search?query=cancer&page=0&size=10"

# 获取文献详情
curl "http://localhost:8080/api/literature/1"

# 获取搜索历史
curl "http://localhost:8080/api/search-history"

# 获取统计信息
curl "http://localhost:8080/api/stats"
```

## 后续优化方向

### 1. 性能优化
- **连接池优化**: 使用HikariCP连接池
- **缓存机制**: 引入Redis缓存
- **异步处理**: 使用@Async注解实现异步爬虫
- **数据库优化**: 添加索引，优化查询

### 2. 功能扩展
- **批量爬取**: 支持批量文献爬取
- **定时任务**: 定期更新文献信息
- **用户系统**: 添加用户注册和登录
- **收藏功能**: 支持文献收藏和分享

### 3. 爬虫增强
- **多数据源**: 支持多个医学数据库
- **智能解析**: 使用机器学习提高解析准确率
- **反爬虫升级**: 动态User-Agent和代理IP
- **数据清洗**: 自动清洗和标准化数据

### 4. 前端优化
- **Vue.js重构**: 使用Vue.js提升交互体验
- **移动端优化**: 专门的移动端界面
- **实时搜索**: 支持实时搜索建议
- **数据可视化**: 图表展示统计信息

## 总结

AI工具在本项目的开发过程中发挥了重要作用，从架构设计到代码实现，从问题诊断到解决方案，都提供了有价值的建议和指导。通过AI的辅助，项目开发效率得到了显著提升，代码质量也得到了保证。

AI辅助开发的优势：
1. **快速原型**: 快速生成基础代码框架
2. **最佳实践**: 提供行业最佳实践建议
3. **问题诊断**: 快速定位和解决问题
4. **代码优化**: 提供性能优化建议
5. **文档生成**: 自动生成技术文档

通过合理使用AI工具，可以让开发者专注于业务逻辑的实现，提高开发效率和代码质量。