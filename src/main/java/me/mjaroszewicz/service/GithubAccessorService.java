package me.mjaroszewicz.service;

import me.mjaroszewicz.storage.TestRepository;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@SuppressWarnings("Duplicates")
@Service
public class GithubAccessorService {

    private final static Logger log = LoggerFactory.getLogger(GithubAccessorService.class);

    @Value("${github.oauth.token}")
    private String oauthToken;

    @Value("${http.user.agent}")
    private String userAgent;

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.zip.url}")
    private String zipUrl; //url to repository zip file

    @Autowired
    private TestRepository repository;

    @Autowired
    private AsyncDownloader asyncDownloader;

    /**
     * Bean initializing method
     */
    @PostConstruct
    private void init(){

        log.info("Initializing Github Accessor");

        if(oauthToken == null || oauthToken.isEmpty()){
            log.error("Fatal! Blank or null oAuth token");
            throw new AssertionError("bad oAuth token");
        }

        if(userAgent == null || userAgent.isEmpty()){
            log.warn("User agent not specified. API may be unnaccessible");
        }


        authorize();

    }

    /**
     * Builds and sends authorization request. If method does not trows any errors, authentication is successful.
     */
    private void authorize() {

        Response response = null;

        try {
            response = asyncDownloader.get("https://api.github.com/").block();
        } catch (Exception e) {
            log.error("Http connection error: " + e);
            throw new IllegalStateException("Could not authorize");
        }

        if (response.getStatusCode() != 200)
            throw new AssertionError("Http response not OK: " + response.getUri().toUrl());

        //printing headers to enable quick diagnostics
        for (Map.Entry<String, String> s : response.getHeaders())
            System.out.println(s.getKey() + " : " + s.getValue());

        String remainingCalls = response.getHeader("X-RateLimit-Remaining");
        String apiResetTime = response.getHeader("X-RateLimit-Reset");

        //formatting reset time to ISO time, ex. '15:30'
        String resetTimeString = LocalDateTime.ofEpochSecond(Long.parseLong(apiResetTime) / 1000, 0, ZoneOffset.ofHours(4)).format(DateTimeFormatter.ISO_TIME);

        log.info("Authorization successful, remaining api requests: " + remainingCalls + ", next Api reset: " + resetTimeString);
    }

    /**
     * Performs series of API calls to Github, parses results and persists results in TestRepository
     * @throws IOException
     */
    public void updateDatabase() throws IOException, ExecutionException, InterruptedException {

        asyncDownloader.go(zipUrl);

    }







}
