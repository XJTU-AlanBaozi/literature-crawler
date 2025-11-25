# AI辅助文献爬虫系统

基于人工智能技术的PubMed医学文献自动爬取与分析平台

## 🎯 项目概述

本项目是一个功能完整的文献爬虫系统，能够自动从PubMed网站爬取医学文献信息，并提供Web界面进行搜索、展示和分析。系统采用Spring Boot框架，结合Selenium和Jsoup技术，实现了高效稳定的数据爬取功能。

## 🚀 主要功能

### 1. 智能爬取功能
- ✅ **PubMed网站爬取**: 自动爬取国外医学文献网站内容
- ✅ **多字段信息提取**: 提取标题、作者、摘要、发表日期等详细信息
- ✅ **深度信息采集**: 使用Selenium技术获取详情页的完整文献信息
- ✅ **机构信息爬取**: 获取作者单位、Affiliation等详细信息

### 2. Web界面功能
- ✅ **智能搜索**: 支持关键词搜索和多字段匹配
- ✅ **分页展示**: 支持搜索结果的分页显示
- ✅ **详情页面**: 点击标题直接跳转到详情页面，清晰展示抓取字段
- ✅ **搜索历史**: 记录和展示用户的搜索历史
- ✅ **响应式设计**: 支持PC和移动端访问

### 3. 数据管理功能
- ✅ **数据库存储**: 使用H2数据库存储文献数据
- ✅ **数据去重**: 基于PMID进行文献去重
- ✅ **缓存机制**: 优先从数据库获取已爬取数据
- ✅ **统计分析**: 提供文献数据统计功能

## 🛠 技术架构

### 后端技术栈
- **框架**: Spring Boot 3.5.8
- **语言**: Java 21
- **数据库**: H2 内存数据库
- **持久层**: Spring Data JPA
- **模板引擎**: Thymeleaf
- **爬虫技术**: Selenium WebDriver 4.15.0 + Jsoup 1.17.2
- **HTTP客户端**: Apache HttpClient 5.2.1
- **JSON处理**: Jackson
- **日志**: SLF4J + Logback

### 前端技术栈
- **CSS框架**: Bootstrap 5.3.0
- **图标库**: Bootstrap Icons
- **JavaScript**: 原生JS + jQuery
- **响应式设计**: 支持多设备适配

## 📦 快速开始

### 环境要求
- Java 21 或更高版本
- Maven 3.6 或更高版本
- Chrome浏览器
- ChromeDriver（需要与Chrome版本匹配）

### 安装步骤

1. **克隆项目**
```bash
git clone <项目地址>
cd literature-crawler
```

2. **配置ChromeDriver**
```bash
# 下载对应版本的ChromeDriver
# 地址: https://chromedriver.chromium.org/
# 配置WebDriverConfig.java中的路径
```

3. **编译项目**
```bash
mvn clean install
```

4. **运行应用**
```bash
mvn spring-boot:run
```

5. **访问系统**
打开浏览器访问: http://localhost:8080

## 📋 使用说明

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
系统提供REST API接口，支持程序化访问：

- `GET /api/search?query={keyword}&page={page}&size={size}` - 搜索文献
- `GET /api/literature/{id}` - 获取文献详情
- `GET /api/search-history` - 获取搜索历史
- `GET /api/stats` - 获取统计信息

## 🔧 配置说明

### 数据库配置
```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb  # H2内存数据库
    driver-class-name: org.h2.Driver
    username: sa
    password: 
  h2:
    console:
      enabled: true
      path: /h2-console  # H2控制台地址
```

### 爬虫配置
```yaml
# 在PubMedCrawlerService中可以配置：
- 请求超时时间（默认30秒）
- 最大搜索结果数量
- ChromeDriver选项
- 请求头配置
```

### 日志配置
```yaml
logging:
  level:
    com.alan.work: DEBUG  # 调试日志级别
    org.springframework.web: INFO
```

## 📊 项目结构

```
src/
├── main/
│   ├── java/com/alan/work/
│   │   ├── config/          # 配置类
│   │   ├── controller/        # 控制器
│   │   ├── entity/           # 实体类
│   │   ├── repository/       # 数据访问层
│   │   ├── service/          # 业务逻辑层
│   │   └── WorkApplication.java  # 主启动类
│   └── resources/
│       ├── templates/         # Thymeleaf模板
│       └── application.yml  # 配置文件
└── test/                    # 测试代码
```

## 🎯 核心功能详解

### 1. 智能爬虫服务
- **PubMedCrawlerService**: 核心爬虫服务，实现两步爬取策略
- **Jsoup解析**: 快速提取列表页基本信息
- **Selenium渲染**: 获取详情页动态加载内容
- **数据清洗**: 自动清洗和格式化爬取的数据

### 2. 搜索服务
- **SearchService**: 智能搜索服务，支持数据库优先策略
- **多字段搜索**: 支持标题、作者、摘要等字段搜索
- **分页支持**: 提供高效的分页查询功能
- **搜索历史**: 记录用户搜索行为

### 3. Web界面
- **响应式设计**: 适配不同设备屏幕
- **用户友好**: 直观的搜索界面和结果展示
- **详情页面**: 独立的详情页面展示完整文献信息
- **加载动画**: 提升用户体验

## 🔍 技术亮点

### 1. AI辅助开发
- 使用AI工具辅助架构设计和技术选型
- 智能代码生成和优化建议
- 问题解决方案的智能推荐

### 2. 高效爬虫技术
- **Jsoup + Selenium** 组合使用，兼顾效率和完整性
- **两步爬取策略**，先快速获取基本信息，再深度获取详情
- **智能反爬虫对策**，包括请求头伪装、随机延迟等

### 3. 稳定可靠的架构
- **Spring Boot** 提供稳定的后端框架
- **分层架构** 确保代码的可维护性
- **异常处理** 完善的错误处理机制
- **日志记录** 详细的运行状态监控

## 🚀 部署说明

### 生产环境部署
1. 配置外部数据库（如MySQL、PostgreSQL）
2. 配置反向代理（如Nginx）
3. 设置环境变量
4. 配置日志文件路径
5. 设置系统服务

### Docker部署（可选）
```dockerfile
FROM openjdk:21-jdk-slim
COPY target/work-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","/app.jar"]
```

## 📈 性能优化

### 爬虫性能优化
- 实现请求限流机制
- 使用连接池优化HTTP请求
- 添加缓存机制减少重复爬取
- 优化ChromeDriver配置

### 数据库性能优化
- 添加合适的索引
- 实现分页查询
- 使用连接池
- 定期清理过期数据

## 🔒 安全考虑

### 数据安全
- 输入验证和SQL注入防护
- XSS攻击防护
- 敏感信息脱敏
- 访问日志记录

### 系统安全
- 定期更新依赖包
- 最小权限原则
- 错误信息脱敏
- 安全头配置

## 📚 相关文档

- [AI使用记录](AI_USAGE.md) - 记录AI工具在项目开发中的使用过程和场景
- [技术文档](Technology.md) - 详细介绍爬虫技术栈和实现细节

## 🤝 贡献指南

1. Fork 项目
2. 创建功能分支 (`git checkout -b feature/amazing-feature`)
3. 提交更改 (`git commit -m 'Add some amazing feature'`)
4. 推送到分支 (`git push origin feature/amazing-feature`)
5. 创建 Pull Request

## 📄 许可证

本项目采用 MIT 许可证 - 查看 [LICENSE](LICENSE) 文件了解详情

## 📞 联系方式

- 项目维护者: Alan
- 邮箱: alan@example.com
- 项目地址: https://github.com/alan/literature-crawler

---

**⭐ 如果这个项目对你有帮助，请给个Star支持一下！**