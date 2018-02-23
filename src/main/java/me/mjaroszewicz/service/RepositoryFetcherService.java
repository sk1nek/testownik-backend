package me.mjaroszewicz.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

@Service
@EnableScheduling
public class RepositoryFetcherService {

    private final static Logger log = LoggerFactory.getLogger(RepositoryFetcherService.class);

    private final Pattern validMetadataLinePattern = Pattern.compile("\\w+&\\w+&.+");


    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.metadata.url}")
    private String metadataUrl;

    @Autowired
    private GithubDownloader downloader;

    @PostConstruct
    private void init() throws AssertionError{

        log.info("Initializing Repository Fetcher Service");
        log.info("GitHub repository address set to: " + repoUrl);

        if(repoUrl == null || !repoUrl.contains("github.com"))
            throw new AssertionError("Provided github repository url does not link to github. ");

    }

    @Scheduled(fixedDelay = 1000L * 60 * 60)
    private void synchronizeTestRepositories() {

        downloader.go(repoUrl);

    }













}
