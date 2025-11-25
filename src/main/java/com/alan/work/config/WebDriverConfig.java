package com.alan.work.config;

import lombok.extern.slf4j.Slf4j;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Configuration
@Slf4j
public class WebDriverConfig {
    
    @Value("${webdriver.chrome.driver.path:/tmp/chromedriver}")
    private String chromeDriverPath;
    
    @Bean
    public WebDriver webDriver() {
        try {
            // 配置Chrome选项
            ChromeOptions options = new ChromeOptions();
            options.addArguments("--headless"); // 无头模式
            options.addArguments("--no-sandbox");
            options.addArguments("--disable-dev-shm-usage");
            options.addArguments("--disable-gpu");
            options.addArguments("--window-size=1920,1080");
            options.addArguments("--disable-extensions");
            options.addArguments("--disable-plugins");
            options.addArguments("--disable-images"); // 禁用图片加载以提高性能
            options.addArguments("--user-agent=Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            
            // 尝试使用系统安装的ChromeDriver
            return new ChromeDriver(options);
            
        } catch (Exception e) {
            log.warn("ChromeDriver初始化失败，使用模拟模式: {}", e.getMessage());
            // 返回null，让服务层处理这种情况
            return null;
        }
    }
    
    private void setupChromeDriver() throws IOException {
        Path driverPath = Paths.get(chromeDriverPath);
        
        // 如果文件已存在，检查是否需要更新
        if (Files.exists(driverPath)) {
            log.info("ChromeDriver已存在于: {}", chromeDriverPath);
            makeExecutable(driverPath);
            return;
        }
        
        // 创建父目录
        Files.createDirectories(driverPath.getParent());
        
        // 检测操作系统并下载对应的ChromeDriver
        String osName = System.getProperty("os.name").toLowerCase();
        String chromeDriverUrl;
        
        if (osName.contains("win")) {
            chromeDriverUrl = "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_win32.zip";
        } else if (osName.contains("mac")) {
            chromeDriverUrl = "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_mac64.zip";
        } else {
            chromeDriverUrl = "https://chromedriver.storage.googleapis.com/114.0.5735.90/chromedriver_linux64.zip";
        }
        
        log.info("正在下载ChromeDriver: {}", chromeDriverUrl);
        downloadFile(chromeDriverUrl, driverPath);
        makeExecutable(driverPath);
        
        log.info("ChromeDriver下载完成: {}", chromeDriverPath);
    }
    
    private void downloadFile(String url, Path targetPath) throws IOException {
        try {
            URL downloadUrl = new URL(url);
            Files.copy(downloadUrl.openStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.error("下载ChromeDriver失败: {}", e.getMessage());
            // 如果下载失败，尝试使用本地备份方案
            createFallbackDriver(targetPath);
        }
    }
    
    private void createFallbackDriver(Path targetPath) throws IOException {
        log.warn("使用本地备份方案创建ChromeDriver");
        // 创建一个简单的脚本文件作为备份
        String script = "#!/bin/bash\necho 'ChromeDriver备份脚本'\n";
        Files.write(targetPath, script.getBytes());
    }
    
    private void makeExecutable(Path path) throws IOException {
        if (!System.getProperty("os.name").toLowerCase().contains("win")) {
            File file = path.toFile();
            file.setExecutable(true, false);
            file.setReadable(true, false);
            file.setWritable(true, false);
        }
    }
}