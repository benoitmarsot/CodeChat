package com.unbumpkin.codechat.service.openai;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;
import edu.uci.ics.crawler4j.url.WebURL;

import com.unbumpkin.codechat.util.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WebCrawlerContentManager extends CCProjectFileManager {
    private File tempDir;
    private String seedUrl;
    private int maxPagesToFetch;
    private int maxDepthOfCrawling;
    private int numberOfCrawlers;
    private Set<String> crawledUrls;
    private List<String> allowedDomains;
    private RateLimit rateLimit;
    private Set<String> excludedFileTypes;

    /**
     * Creates a new WebCrawlerContentManager with default settings.
     */
    public WebCrawlerContentManager() throws IOException {
        this(100,2,4,30,true,true);
    }

    /**
     * Creates a new WebCrawlerContentManager with custom settings.
     *
     * @param maxPagesToFetch Maximum number of pages to fetch
     * @param maxDepthOfCrawling Maximum depth of crawling
     * @param numberOfCrawlers Number of crawler threads
     * @param requestsPerMinute Maximum number of requests per minute (rate limiting)
     */
    public WebCrawlerContentManager(
        int maxPagesToFetch, int maxDepthOfCrawling, int numberOfCrawlers, int requestsPerMinute,
        boolean includeImages, boolean includeDocuments
    ) throws IOException {
        super();
        this.tempDir = Files.createTempDirectory("web-crawl-").toFile();
        this.maxPagesToFetch = maxPagesToFetch;
        this.maxDepthOfCrawling = maxDepthOfCrawling;
        this.numberOfCrawlers = numberOfCrawlers;
        this.crawledUrls = new HashSet<>();
        this.allowedDomains = new ArrayList<>();
        this.rateLimit = new RateLimit(requestsPerMinute, 1);
        this.excludedFileTypes = new HashSet<>();
        
        // If not including documents/images, add their extensions to excluded types
        if (!includeDocuments) {
            this.excludedFileTypes.addAll(List.of("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx"));
        }
        if (!includeImages) {
            this.excludedFileTypes.addAll(List.of("jpg", "jpeg", "png", "gif", "bmp"));
        }
        
        // Always exclude these types
        this.excludedFileTypes.addAll(List.of(
            "zip", "rar", "gz", "tar", "mp3", "mp4", "avi", "mov", "wmv",
            "sqlite","db","db3","sqlite3"
        ));
    }
    // Add a method to exclude specific file types
    public void excludeFileTypes(String... fileTypes) {
        if (fileTypes != null) {
            for (String type : fileTypes) {
                excludedFileTypes.add(type.toLowerCase());
            }
        }
    }
    
    // Add a method to include specific file types
    public void includeFileTypes(String... fileTypes) {
        if (fileTypes != null) {
            for (String type : fileTypes) {
                excludedFileTypes.remove(type.toLowerCase());
            }
        }
    }
    /**
     * Starts crawling from the given seed URL.
     *
     * @param seedUrl The starting URL for crawling
     * @return The path to the directory containing crawled content
     * @throws Exception If an error occurs during crawling
     */
    public String crawlWebsite(String seedUrl) throws Exception {

        this.seedUrl = seedUrl;
        return crawlWebsite(seedUrl, null);
    }

    /**
     * Gets the seed URL.
     * @return The seed URL
     */
    public String getSeedUrl() {
        return seedUrl;
    }
    /**
     * Starts crawling from the given seed URL, restricting to specified domains.
     *
     * @param seedUrl The starting URL for crawling
     * @param allowDomains List of allowed domains to crawl, or null to allow only the seed domain
     * @return The path to the directory containing crawled content
     * @throws Exception If an error occurs during crawling
     */
    public String crawlWebsite(String seedUrl, List<String> allowDomains) throws Exception {
        this.seedUrl = seedUrl;
        
        // Set up the crawl controller
        CrawlConfig config = new CrawlConfig();
        config.setCrawlStorageFolder(tempDir.getAbsolutePath());
        config.setMaxPagesToFetch(maxPagesToFetch);
        config.setMaxDepthOfCrawling(maxDepthOfCrawling);
        config.setPolitenessDelay(rateLimit.getDelayMs());
        config.setIncludeBinaryContentInCrawling(true);
        config.setResumableCrawling(false);
        
        // Extract base domain from seed URL
        String baseDomain = extractDomain(seedUrl);
        
        // Set allowed domains
        if (allowDomains != null && !allowDomains.isEmpty()) {
            this.allowedDomains = allowDomains;
        } else {
            // By default, only allow crawling the same domain as the seed URL
            this.allowedDomains = new ArrayList<>();
            this.allowedDomains.add(baseDomain);
        }
        
        // Initialize the controller
        PageFetcher pageFetcher = new PageFetcher(config);
        RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
        RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
        CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
        
        // Add the seed URL
        controller.addSeed(seedUrl);
        
        // Set up the factory
        CrawlController.WebCrawlerFactory<WebsiteCrawler> factory = () -> 
            new WebsiteCrawler(this, this.allowedDomains);
        
        // Start the crawl
        controller.start(factory, numberOfCrawlers);
        
        // Use the inherited method to scan and categorize files
        super.addDir(tempDir.getAbsolutePath());
        
        return tempDir.getAbsolutePath();
    }

    /**
     * Extracts the domain from a URL
     */
    private String extractDomain(String url) {
        String domain = url.toLowerCase();
        if (domain.startsWith("http://")) {
            domain = domain.substring(7);
        } else if (domain.startsWith("https://")) {
            domain = domain.substring(8);
        }
        
        int slashIndex = domain.indexOf('/');
        if (slashIndex != -1) {
            domain = domain.substring(0, slashIndex);
        }
        
        // Remove www. prefix if present
        if (domain.startsWith("www.")) {
            domain = domain.substring(4);
        }
        
        return domain;
    }
    
    /**
     * Saves crawled content to a file in the temp directory.
     */
    protected void saveContent(String url, String contentType, String content) {
        try {
            // Create a file name based on the URL
            String fileName = urlToFileName(url, contentType);
            File outputFile = new File(tempDir, fileName);
            
            // Create parent directories if needed
            outputFile.getParentFile().mkdirs();
            
            // Save the content to the file
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile))) {
                writer.write(content);
            }
            
            // Add the file to our manager
            addFile(outputFile);
            
            // Record the URL
            crawledUrls.add(url);
            
        } catch (IOException e) {
            System.err.println("Error saving content for URL: " + url + " - " + e.getMessage());
        }
    }
    
    /**
     * Saves binary content to a file in the temp directory.
     */
    protected void saveBinaryContent(String url, String contentType, byte[] content) {
        try {
            // Create a file name based on the URL
            String fileName = urlToFileName(url, contentType);
            File outputFile = new File(tempDir, fileName);
            
            // Create parent directories if needed
            outputFile.getParentFile().mkdirs();
            
            // Save the binary content to the file
            Files.write(outputFile.toPath(), content);
            
            // Add the file to our manager
            addFile(outputFile);
            
            // Record the URL
            crawledUrls.add(url);
            
        } catch (IOException e) {
            System.err.println("Error saving binary content for URL: " + url + " - " + e.getMessage());
        }
    }
    
    /**
     * Converts a URL to a valid file name.
     */
    private String urlToFileName(String url, String contentType) {
        // Remove http:// or https:// prefix
        String fileName = url.replaceFirst("^(http|https)://", "");
        
        // Replace invalid characters with underscores
        fileName = fileName.replaceAll("[\\\\/:*?\"<>|]", "_");
        
        // Handle the file extension based on content type if missing
        if (contentType != null) {
            if (contentType.contains("text/html") && !fileName.endsWith(".html")) {
                fileName += ".html";
            } else if (contentType.contains("text/css") && !fileName.endsWith(".css")) {
                fileName += ".css";
            } else if (contentType.contains("application/javascript") && !fileName.endsWith(".js")) {
                fileName += ".js";
            } else if (contentType.contains("image/jpeg") && !fileName.endsWith(".jpg")) {
                fileName += ".jpg";
            } else if (contentType.contains("image/png") && !fileName.endsWith(".png")) {
                fileName += ".png";
            } else if (contentType.contains("image/gif") && !fileName.endsWith(".gif")) {
                fileName += ".gif";
            } else if (contentType.contains("application/json") && !fileName.endsWith(".json")) {
                fileName += ".json";
            } else if ((contentType.contains("application/xml") || contentType.contains("text/xml"))
                    && !fileName.endsWith(".xml")
            ) {
                fileName += ".xml";
            }
        }
        
        return fileName;
    }
    
    /**
     * Cleans up temporary directories and resources.
     */
    public void cleanup() {
        if (tempDir != null && tempDir.exists()) {
            try {
                FileUtils.deleteDirectory(tempDir);
            } catch (IOException e) {
                System.err.println("Failed to delete temporary directory: " + e.getMessage());
            }
        }
    }
    
    @Override
    public String getTempDir() {
        return tempDir.getAbsolutePath();
    }
    
    /**
     * Gets the set of URLs that were crawled.
     */
    public Set<String> getCrawledUrls() {
        return crawledUrls;
    }
    
    /**
     * Gets the list of allowed domains.
     */
    public List<String> getAllowedDomains() {
        return allowedDomains;
    }
    
    /**
     * Sets the maximum number of pages to fetch.
     */
    public void setMaxPagesToFetch(int maxPagesToFetch) {
        this.maxPagesToFetch = maxPagesToFetch;
    }
    
    /**
     * Sets the maximum depth of crawling.
     */
    public void setMaxDepthOfCrawling(int maxDepthOfCrawling) {
        this.maxDepthOfCrawling = maxDepthOfCrawling;
    }
    
    /**
     * Sets the number of crawler threads.
     */
    public void setNumberOfCrawlers(int numberOfCrawlers) {
        this.numberOfCrawlers = numberOfCrawlers;
    }
    
    /**
     * Sets the rate limit (requests per minute).
     */
    public void setRateLimit(int requestsPerMinute) {
        this.rateLimit = new RateLimit(requestsPerMinute, 1);
    }
    
    /**
     * Inner class that extends WebCrawler to handle the actual crawling.
     */
    private static class WebsiteCrawler extends WebCrawler {
        private final WebCrawlerContentManager manager;
        private final List<String> allowedDomains;
        
        public WebsiteCrawler(WebCrawlerContentManager manager, List<String> allowedDomains) {
            this.manager = manager;
            this.allowedDomains = allowedDomains;
        }
        
        @Override
        public boolean shouldVisit(Page referringPage, WebURL url) {
            String href = url.getURL().toLowerCase();
            
            // Check if the URL ends with any excluded extension
            for (String ext : manager.excludedFileTypes) {
                if (href.endsWith("." + ext)) {
                    return false;
                }
            }
            
            // Check if the domain is allowed
            String domain = manager.extractDomain(href);
            for (String allowedDomain : allowedDomains) {
                if (domain.contains(allowedDomain) || allowedDomain.contains(domain)) {
                    return true;
                }
            }
            
            return false;
        }
        
        @Override
        public void visit(Page page) {
            String url = page.getWebURL().getURL();
            String contentType = page.getContentType();
            
            if (contentType != null && contentType.contains("text/html")) {
                // Process HTML
                if (page.getParseData() instanceof HtmlParseData) {
                    HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
                    String html = htmlParseData.getHtml();
                    manager.saveContent(url, contentType, html);
                }
            } else if (contentType != null && (contentType.contains("text/css") || 
                      contentType.contains("application/javascript") || 
                      contentType.contains("application/json") || 
                      contentType.contains("text/plain") ||
                      contentType.contains("application/xml") || 
                      contentType.contains("text/xml"))) {
                // Process text-based files
                String text = new String(page.getContentData(), StandardCharsets.UTF_8);
                manager.saveContent(url, contentType, text);
            } else if (contentType != null && contentType.startsWith("image/")) {
                // Process images
                manager.saveBinaryContent(url, contentType, page.getContentData());
            }
        }
    }
    
    /**
     * Class to handle rate limiting.
     */
    public static class RateLimit {
        private final int requestsPerTimeUnit;
        private final int timeUnitMinutes;
        
        public RateLimit(int requestsPerTimeUnit, int timeUnitMinutes) {
            this.requestsPerTimeUnit = requestsPerTimeUnit;
            this.timeUnitMinutes = timeUnitMinutes;
        }
        
        /**
         * Gets the delay in milliseconds between requests to respect the rate limit.
         */
        public int getDelayMs() {
            return (timeUnitMinutes * 60 * 1000) / requestsPerTimeUnit;
        }
    }
    
    /**
     * Class for rate limit exceptions.
     */
    public static class RateLimitExceededException extends Exception {
        private final long waitTimeMs;
        
        public RateLimitExceededException(String message, long waitTimeMs) {
            super(message);
            this.waitTimeMs = waitTimeMs;
        }
        
        public long getWaitTimeMs() {
            return waitTimeMs;
        }
    }
}