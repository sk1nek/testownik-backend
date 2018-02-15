package me.mjaroszewicz.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

@Service
public class GithubAccessorService {

    private final static Logger log = LoggerFactory.getLogger(GithubAccessorService.class);

    private HttpClient httpClient;

    @Value("${github.oauth.token}")
    private String oauthToken;

    @Value("${http.user.agent}")
    private String userAgent;

    @Value("${github.repo.url}")
    private String repoUrl;

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

        //creating http client
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setUserAgent(userAgent);

        httpClient = builder.build();

        authorize();

    }

    /**
     * Builds and sends authorization request. If method does not trows any errors, authentication is successful.
     */
    private void authorize() {
        HttpUriRequest request = RequestBuilder.get("https://api.github.com/").addParameter("access_token", oauthToken).build();

        HttpResponse response = null;
        try {
            response = httpClient.execute(request);
        } catch (IOException e) {
            log.error("Http connection error: " + e);
            throw new IllegalStateException("Could not authorize");
        }

        if (response.getStatusLine().getStatusCode() != 200) {
            throw new AssertionError("Http response not OK");
        }

        for (Header header : response.getAllHeaders()) {
            System.out.println(header.toString());
        }

        Header remainingCalls = response.getFirstHeader("X-RateLimit-Remaining");
        Header apiResetTime = response.getFirstHeader("X-RateLimit-Reset");

        //formatting reset time to ISO time, ex. '15:30'
        String resetTimeString = LocalDateTime.ofEpochSecond(Long.parseLong(apiResetTime.getValue()) / 1000, 0, ZoneOffset.UTC).format(DateTimeFormatter.ISO_TIME);

        log.info("Authorization successful, remaining api requests: " + remainingCalls.getValue() + ", next Api reset: " + resetTimeString);

    }

    public void updateDatabase(){

        String contentsUrl = repoUrl + "contents";


    }

}
