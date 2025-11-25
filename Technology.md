# 技术文档

## 爬虫技术栈概述

### 核心技术组件
- **Jsoup 1.17.2**: Java HTML解析器，用于静态页面解析
- **Selenium WebDriver 4.15.0**: 浏览器自动化工具，用于动态页面渲染
- **Apache HttpClient 5.2.1**: HTTP客户端，用于发送网络请求
- **ChromeDriver**: Chrome浏览器驱动，配合Selenium使用

### 辅助技术
- **XPath**: XML路径语言，用于精确定位HTML元素
- **CSS选择器**: 层叠样式表选择器，用于元素选择
- **正则表达式**: 用于文本模式匹配和数据提取
- **Jackson**: JSON数据处理库

## 核心技术组件使用方法

### 1. Jsoup使用详解

#### 基本连接和解析
```java
// 建立连接
Connection connection = Jsoup.connect("https://pubmed.ncbi.nlm.nih.gov/12345678");
connection.userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
connection.timeout(30000); // 30秒超时

// 获取文档对象
Document doc = connection.get();

// 解析HTML
String title = doc.title();
String bodyText = doc.body().text();
```

#### CSS选择器使用
```java
// 选择文章标题
Element titleElement = doc.select("h1.heading-title").first();
String title = titleElement != null ? titleElement.text() : "";

// 选择作者列表
Elements authorElements = doc.select(".authors-list .full-name");
List<String> authors = authorElements.stream()
    .map(Element::text)
    .collect(Collectors.toList());

// 选择摘要内容
Element abstractElement = doc.select("#abstract-content").first();
String abstractText = abstractElement != null ? abstractElement.text() : "";
```

#### 属性提取
```java
// 获取链接地址
Element linkElement = doc.select("a.link-item").first();
String href = linkElement.attr("href");

// 获取图片地址
Element imgElement = doc.select("img.figure-image").first();
String imgSrc = imgElement.attr("src");

// 获取DOI
Element doiElement = doc.select("[data-doi]").first();
String doi = doiElement.attr("data-doi");
```

### 2. Selenium WebDriver使用详解

#### WebDriver配置
```java
public WebDriver createWebDriver() {
    ChromeOptions options = new ChromeOptions();
    
    // 无头模式（不显示浏览器窗口）
    options.addArguments("--headless");
    
    // 禁用GPU加速
    options.addArguments("--disable-gpu");
    
    // 禁用沙盒模式
    options.addArguments("--no-sandbox");
    
    // 禁用开发者工具
    options.addArguments("--disable-dev-shm-usage");
    
    // 设置窗口大小
    options.addArguments("--window-size=1920,1080");
    
    // 禁用图片加载（提升性能）
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("profile.managed_default_content_settings.images", 2);
    options.setExperimentalOption("prefs", prefs);
    
    return new ChromeDriver(options);
}
```

#### 页面等待策略
```java
// 隐式等待
webDriver.manage().timeouts().implicitlyWait(10, TimeUnit.SECONDS);

// 显式等待
WebDriverWait wait = new WebDriverWait(webDriver, 10);
wait.until(ExpectedConditions.presenceOfElementLocated(By.id("content")));

// 等待元素可见
wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector(".article-content")));

// 等待元素可点击
wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector(".download-button")));
```

#### 元素定位和交互
```java
// 通过ID定位
WebElement element = webDriver.findElement(By.id("search-input"));

// 通过CSS选择器定位
WebElement element = webDriver.findElement(By.cssSelector(".search-button"));

// 通过XPath定位
WebElement element = webDriver.findElement(By.xpath("//div[@class='content']/p[1]"));

// 输入文本
element.sendKeys("search keyword");

// 点击元素
element.click();

// 获取文本内容
String text = element.getText();

// 获取属性值
String attribute = element.getAttribute("href");
```

#### JavaScript执行
```java
// 执行JavaScript
JavascriptExecutor js = (JavascriptExecutor) webDriver;
js.executeScript("window.scrollTo(0, document.body.scrollHeight);");

// 获取页面高度
Long pageHeight = (Long) js.executeScript("return document.body.scrollHeight;");

// 修改元素属性
js.executeScript("arguments[0].setAttribute('data-value', 'new-value');", element);
```

### 3. XPath使用技巧

#### 基本语法
```java
// 选择所有div元素
//div

// 选择id为content的div
//div[@id='content']

// 选择class包含article的元素
//*[contains(@class, 'article')]

// 选择第一个p元素
//p[1]

// 选择最后一个div元素
//div[last()]
```

#### 高级用法
```java
// 选择文本包含特定内容的元素
//*[contains(text(), 'Abstract')]

// 选择具有特定子元素的父元素
//div[.//span[@class='title']]

// 选择同级元素
//div[@class='content']/following-sibling::div[1]

// 选择父元素
//span[@class='title']/parent::div
```

## 两步爬取策略

### 第一步：列表页爬取（Jsoup）

#### 目标
快速获取搜索结果列表中的基本信息

#### 实现
```java
public List<Literature> crawlSearchResults(String query) {
    String searchUrl = PUBMED_SEARCH_URL + URLEncoder.encode(query, "UTF-8");
    Document doc = Jsoup.connect(searchUrl)
        .userAgent(USER_AGENT)
        .timeout(30000)
        .get();
    
    List<Literature> results = new ArrayList<>();
    Elements articleElements = doc.select(".docsum-content");
    
    for (Element article : articleElements) {
        Literature literature = new Literature();
        
        // 提取PMID
        Element pmidElement = article.select(".docsum-pmid").first();
        literature.setPmid(pmidElement.text());
        
        // 提取标题
        Element titleElement = article.select(".docsum-title").first();
        literature.setTitle(titleElement.text());
        
        // 提取作者
        Element authorElement = article.select(".docsum-authors").first();
        literature.setAuthors(authorElement.text());
        
        // 提取来源
        Element sourceElement = article.select(".docsum-journal").first();
        literature.setJournal(sourceElement.text());
        
        results.add(literature);
    }
    
    return results;
}
```

### 第二步：详情页爬取（Selenium）

#### 目标
获取每篇文献的详细信息

#### 实现
```java
public Literature crawlLiteratureDetail(String pmid) {
    WebDriver webDriver = createWebDriver();
    try {
        String detailUrl = PUBMED_DETAIL_URL + pmid;
        webDriver.get(detailUrl);
        
        // 等待页面加载
        WebDriverWait wait = new WebDriverWait(webDriver, 10);
        wait.until(ExpectedConditions.presenceOfElementLocated(
            By.cssSelector(".abstract-content")
        ));
        
        // 获取完整页面源代码
        String pageSource = webDriver.getPageSource();
        Document doc = Jsoup.parse(pageSource);
        
        Literature literature = new Literature();
        literature.setPmid(pmid);
        
        // 提取详细信息
        literature.setTitle(extractTitle(doc));
        literature.setAuthors(extractAuthors(doc));
        literature.setAbstractText(extractAbstract(doc));
        literature.setDoi(extractDOI(doc));
        literature.setInstitution(extractInstitution(doc));
        literature.setFullTextLinks(extractFullTextLinks(doc));
        
        return literature;
        
    } finally {
        webDriver.quit();
    }
}
```

## 反爬虫对策

### 1. 请求头配置

#### 完整的请求头设置
```java
private static final Map<String, String> REQUEST_HEADERS = Map.of(
    "User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
    "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
    "Accept-Language", "en-US,en;q=0.5",
    "Accept-Encoding", "gzip, deflate, br",
    "Connection", "keep-alive",
    "Upgrade-Insecure-Requests", "1",
    "Sec-Fetch-Dest", "document",
    "Sec-Fetch-Mode", "navigate",
    "Sec-Fetch-Site", "none",
    "Cache-Control", "max-age=0"
);

public Connection configureConnection(String url) {
    Connection connection = Jsoup.connect(url);
    
    // 设置请求头
    REQUEST_HEADERS.forEach(connection::header);
    
    // 设置超时
    connection.timeout(30000);
    
    // 设置跟随重定向
    connection.followRedirects(true);
    
    // 忽略内容类型
    connection.ignoreContentType(true);
    
    return connection;
}
```

### 2. 请求限流

#### 随机延迟实现
```java
public class RateLimiter {
    private static final int MIN_DELAY = 1000;  // 最小延迟1秒
    private static final int MAX_DELAY = 3000;  // 最大延迟3秒
    
    private final Random random = new Random();
    
    public void randomDelay() {
        int delay = MIN_DELAY + random.nextInt(MAX_DELAY - MIN_DELAY);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    public void exponentialBackoff(int attempt) {
        long delay = (long) (Math.pow(2, attempt) * 1000);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

### 3. 代理支持

#### 代理配置
```java
public class ProxyManager {
    private final List<Proxy> proxies = new ArrayList<>();
    private int currentIndex = 0;
    
    public void addProxy(String host, int port) {
        proxies.add(new Proxy(Proxy.Type.HTTP, new InetSocketAddress(host, port)));
    }
    
    public Proxy getNextProxy() {
        if (proxies.isEmpty()) {
            return Proxy.NO_PROXY;
        }
        
        Proxy proxy = proxies.get(currentIndex);
        currentIndex = (currentIndex + 1) % proxies.size();
        return proxy;
    }
    
    public Jsoup configureProxy(Jsoup connection) {
        Proxy proxy = getNextProxy();
        return connection.proxy(proxy);
    }
}
```

## 数据清洗和验证

### 1. 文本清洗

#### 去除多余空白
```java
public String cleanText(String text) {
    if (text == null) {
        return "";
    }
    
    return text.trim()
        .replaceAll("\\s+", " ")  // 多个空白字符替换为一个空格
        .replaceAll("\\n+", "\\n")  // 多个换行替换为一个
        .replaceAll("[\\u00A0|\\u202F|\\u2007]", " ");  // 替换特殊空白字符
}
```

#### HTML标签清理
```java
public String removeHtmlTags(String html) {
    if (html == null) {
        return "";
    }
    
    return html.replaceAll("<[^>]*>", "")  // 移除HTML标签
        .replaceAll("&[^;]+;", "")  // 移除HTML实体
        .trim();
}
```

### 2. 数据验证

#### PMID格式验证
```java
public boolean isValidPmid(String pmid) {
    if (pmid == null || pmid.trim().isEmpty()) {
        return false;
    }
    
    // PMID应该是1-8位的数字
    return pmid.matches("^\\d{1,8}$");
}
```

#### DOI格式验证
```java
public boolean isValidDoi(String doi) {
    if (doi == null || doi.trim().isEmpty()) {
        return false;
    }
    
    // DOI基本格式验证
    return doi.matches("^10\\.\\d{4,}/.+$");
}
```

#### 日期格式验证
```java
public boolean isValidDate(String date) {
    if (date == null || date.trim().isEmpty()) {
        return false;
    }
    
    try {
        // 尝试解析日期
        LocalDate.parse(date, DateTimeFormatter.ISO_DATE);
        return true;
    } catch (DateTimeParseException e) {
        return false;
    }
}
```

### 3. 数据标准化

#### 作者姓名标准化
```java
public String normalizeAuthorName(String name) {
    if (name == null || name.trim().isEmpty()) {
        return "";
    }
    
    // 移除多余空格
    name = name.trim().replaceAll("\\s+", " ");
    
    // 首字母大写
    return Arrays.stream(name.split(" "))
        .map(word -> word.isEmpty() ? word : 
            word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
        .collect(Collectors.joining(" "));
}
```

#### 机构名称标准化
```java
public String normalizeInstitution(String institution) {
    if (institution == null || institution.trim().isEmpty()) {
        return "";
    }
    
    // 移除多余空格和换行
    institution = institution.trim().replaceAll("\\s+", " ");
    
    // 移除常见的机构前缀
    institution = institution.replaceAll("^(Department of|Division of|School of|University of)\\s+", "");
    
    return institution;
}
```

## 性能优化

### 1. 连接池优化

#### HTTP连接池配置
```java
public class HttpClientConfig {
    
    public CloseableHttpClient createHttpClient() {
        // 连接池管理器
        PoolingHttpClientConnectionManager connectionManager = 
            new PoolingHttpClientConnectionManager();
        
        // 设置最大连接数
        connectionManager.setMaxTotal(100);
        
        // 设置每个路由的最大连接数
        connectionManager.setDefaultMaxPerRoute(20);
        
        // 连接超时配置
        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(5000)      // 连接超时5秒
            .setSocketTimeout(30000)      // 读取超时30秒
            .setConnectionRequestTimeout(1000)  // 连接请求超时1秒
            .build();
        
        return HttpClients.custom()
            .setConnectionManager(connectionManager)
            .setDefaultRequestConfig(requestConfig)
            .build();
    }
}
```

### 2. 禁用图片和JavaScript

#### Chrome选项配置
```java
public ChromeOptions createPerformanceOptions() {
    ChromeOptions options = new ChromeOptions();
    
    // 禁用图片加载
    Map<String, Object> prefs = new HashMap<>();
    prefs.put("profile.managed_default_content_settings.images", 2);
    options.setExperimentalOption("prefs", prefs);
    
    // 禁用JavaScript（如果不需要）
    // options.addArguments("--disable-javascript");
    
    // 禁用CSS（如果不需要）
    // options.addArguments("--disable-css");
    
    // 禁用插件
    options.addArguments("--disable-plugins");
    
    // 禁用扩展
    options.addArguments("--disable-extensions");
    
    return options;
}
```

### 3. 缓存机制

#### 本地缓存实现
```java
public class CrawlerCache {
    private final Map<String, Literature> cache = new ConcurrentHashMap<>();
    private final long cacheExpirationTime = 24 * 60 * 60 * 1000; // 24小时
    
    public void put(String pmid, Literature literature) {
        cache.put(pmid, literature);
    }
    
    public Literature get(String pmid) {
        return cache.get(pmid);
    }
    
    public boolean contains(String pmid) {
        return cache.containsKey(pmid);
    }
    
    public void clear() {
        cache.clear();
    }
    
    public void removeExpired() {
        // 移除过期缓存项
        cache.entrySet().removeIf(entry -> {
            // 检查是否过期
            return isExpired(entry.getValue());
        });
    }
    
    private boolean isExpired(Literature literature) {
        // 检查文献是否过期
        return System.currentTimeMillis() - literature.getCreatedAt() > cacheExpirationTime;
    }
}
```

## 错误处理和重试机制

### 1. 异常分类处理

#### 爬虫异常处理
```java
public class CrawlerExceptionHandler {
    
    public Literature handleCrawlException(String pmid, Exception e) {
        log.error("爬取文献 {} 失败: {}", pmid, e.getMessage());
        
        if (e instanceof SocketTimeoutException) {
            // 连接超时，尝试重试
            return retryCrawl(pmid);
        } else if (e instanceof UnknownHostException) {
            // DNS解析失败，稍后重试
            throw new CrawlerException("DNS解析失败，请检查网络连接");
        } else if (e instanceof SSLException) {
            // SSL证书问题，忽略证书验证
            return crawlWithInsecureSSL(pmid);
        } else {
            // 其他异常，记录并返回空结果
            return createEmptyLiterature(pmid);
        }
    }
    
    private Literature retryCrawl(String pmid) {
        // 重试逻辑
        int maxRetries = 3;
        for (int i = 1; i <= maxRetries; i++) {
            try {
                Thread.sleep(i * 2000); // 指数退避
                return crawlLiterature(pmid);
            } catch (Exception e) {
                log.warn("第 {} 次重试失败: {}", i, e.getMessage());
                if (i == maxRetries) {
                    throw new CrawlerException("重试" + maxRetries + "次后仍然失败");
                }
            }
        }
        return null;
    }
}
```

### 2. 重试策略

#### 指数退避重试
```java
@Retryable(
    value = {Exception.class}, 
    maxAttempts = 3, 
    backoff = @Backoff(
        delay = 1000, 
        multiplier = 2, 
        maxDelay = 10000
    )
)
public Literature crawlWithRetry(String pmid) {
    return crawlLiterature(pmid);
}
```

#### 自定义重试模板
```java
public class RetryTemplate {
    
    public <T> T execute(RetryCallback<T> callback, int maxAttempts) {
        Exception lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                return callback.doWithRetry();
            } catch (Exception e) {
                lastException = e;
                log.warn("第 {} 次尝试失败: {}", attempt, e.getMessage());
                
                if (attempt < maxAttempts) {
                    // 指数退避
                    long delay = (long) Math.pow(2, attempt) * 1000;
                    try {
                        Thread.sleep(delay);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("重试被中断", ie);
                    }
                }
            }
        }
        
        throw new RuntimeException("重试" + maxAttempts + "次后仍然失败", lastException);
    }
}
```

## 监控和日志

### 1. 爬虫监控

#### 爬虫统计信息
```java
@Component
public class CrawlerStatistics {
    private final AtomicInteger totalRequests = new AtomicInteger(0);
    private final AtomicInteger successfulRequests = new AtomicInteger(0);
    private final AtomicInteger failedRequests = new AtomicInteger(0);
    private final AtomicLong totalCrawlTime = new AtomicLong(0);
    
    public void recordRequest(boolean success, long duration) {
        totalRequests.incrementAndGet();
        if (success) {
            successfulRequests.incrementAndGet();
        } else {
            failedRequests.incrementAndGet();
        }
        totalCrawlTime.addAndGet(duration);
    }
    
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalRequests", totalRequests.get());
        stats.put("successfulRequests", successfulRequests.get());
        stats.put("failedRequests", failedRequests.get());
        stats.put("successRate", 
            totalRequests.get() > 0 ? 
            (double) successfulRequests.get() / totalRequests.get() * 100 : 0);
        stats.put("averageCrawlTime", 
            successfulRequests.get() > 0 ? 
            totalCrawlTime.get() / successfulRequests.get() : 0);
        
        return stats;
    }
}
```

### 2. 详细日志记录

#### 爬虫日志配置
```java
@Slf4j
@Service
public class LoggingCrawlerService {
    
    public Literature crawlLiterature(String pmid) {
        log.info("开始爬取文献: {}", pmid);
        long startTime = System.currentTimeMillis();
        
        try {
            Literature literature = performCrawl(pmid);
            long duration = System.currentTimeMillis() - startTime;
            
            log.info("成功爬取文献: {}, 耗时: {}ms, 标题: {}", 
                pmid, duration, literature.getTitle());
            
            return literature;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("爬取文献失败: {}, 耗时: {}ms, 错误: {}", 
                pmid, duration, e.getMessage(), e);
            throw new CrawlerException("爬取失败: " + e.getMessage(), e);
        }
    }
    
    private Literature performCrawl(String pmid) {
        // 实际爬取逻辑
        return new Literature();
    }
}
```

## 最佳实践总结

### 1. 代码组织
- **分层架构**: Controller → Service → Repository
- **单一职责**: 每个类和方法只负责一个功能
- **异常处理**: 完善的异常捕获和处理机制
- **日志记录**: 详细的运行状态监控

### 2. 性能优化
- **连接池**: 使用HTTP连接池减少连接开销
- **缓存机制**: 实现多级缓存减少重复请求
- **异步处理**: 使用异步编程提高并发能力
- **资源管理**: 及时释放WebDriver等资源

### 3. 稳定性保障
- **重试机制**: 自动重试失败的操作
- **限流控制**: 避免过于频繁的请求
- **异常恢复**: 优雅处理各种异常情况
- **监控告警**: 实时监控爬虫状态

### 4. 数据质量
- **数据验证**: 对爬取的数据进行格式验证
- **数据清洗**: 清理和标准化数据格式
- **去重机制**: 避免重复数据的存储
- **增量更新**: 只更新变化的数据

### 5. 安全防护
- **请求伪装**: 模拟真实浏览器行为
- **代理使用**: 使用代理IP避免被封
- **频率控制**: 控制请求频率避免触发反爬
- **异常处理**: 不暴露系统内部信息

通过遵循这些最佳实践，可以构建一个高效、稳定、安全的文献爬虫系统。