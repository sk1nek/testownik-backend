package me.mjaroszewicz.service;

import me.mjaroszewicz.storage.TestRepository;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

@Service
@EnableScheduling
public class RepositoryFetcherService {

    private final static Logger log = LoggerFactory.getLogger(RepositoryFetcherService.class);

    @Autowired
    private ParserService parserService;

    @Autowired
    private HttpDownloader httpDownloader;

    @Value("${github.repo.url}")
    private String repoUrl;

    @PostConstruct
    private void init() throws AssertionError{

        log.info("Initializing Repository Fetcher Service");
        log.info("GitHub repository address set to: " + repoUrl);

        if(repoUrl == null || !repoUrl.contains("https://github.com/"))
            throw new AssertionError("Provided github repository url does not link to github. ");

    }

    @Scheduled(fixedDelay = 1000L * 60 * 60)
    private void synchronizeTestRepositories() throws IOException {

        parserService.parseRepositoryData(httpDownloader.getStringFromUrl(repoUrl));

    }








}
