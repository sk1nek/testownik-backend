package me.mjaroszewicz.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.mjaroszewicz.entities.Question;
import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.TestRepository;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
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
import java.util.*;
import java.util.stream.Collectors;

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

    @Autowired
    private TestRepository repository;

    @Autowired
    private HttpDownloader httpDownloader;

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

        HttpResponse response = null;

        try {
            response = httpDownloader.getUrl("http://api.github.com/");
        } catch (IOException e) {
            log.error("Http connection error: " + e);
            throw new IllegalStateException("Could not authorize");
        }

        if (response.getStatusLine().getStatusCode() != 200)
            throw new AssertionError("Http response not OK");


        //printing headers to enable quick diagnostics
        for (Header header : response.getAllHeaders())
            System.out.println(header.toString());

        Header remainingCalls = response.getFirstHeader("X-RateLimit-Remaining");
        Header apiResetTime = response.getFirstHeader("X-RateLimit-Reset");

        //formatting reset time to ISO time, ex. '15:30'
        String resetTimeString = LocalDateTime.ofEpochSecond(Long.parseLong(apiResetTime.getValue()) / 1000, 0, ZoneOffset.ofHours(4)).format(DateTimeFormatter.ISO_TIME);

        log.info("Authorization successful, remaining api requests: " + remainingCalls.getValue() + ", next Api reset: " + resetTimeString);

    }

    /**
     * Performs series of API calls to Github, parses results and persists results in TestRepository
     * @throws IOException
     */
    public void updateDatabase() throws IOException{

        String contentsUrl = repoUrl + "contents";

        List<Test> tests = new ArrayList<>();

        System.out.println("--");

        httpDownloader.getContentRoot(contentsUrl)
                .parallelStream()
                .map(this::testFromDirectory)
                .filter(Test.class::isInstance)
                .forEach(tests::add);

        repository.addAll(tests);

        System.out.println("finish");
    }

    /**
     *
     * @param dir
     * @return null if http request was unsuccessful
     */
    private Test testFromDirectory(JsonObject dir)  {

        HttpDownloader downloader = new HttpDownloader();

        String defaultDescription = "Default description";
        String id = dir.get("path").getAsString();
        String defaultTitle = id;

        Test ret = new Test(defaultTitle, id, defaultDescription, Collections.emptyList());

        String directoryRootUrl = dir.get("url").getAsString();

        List<JsonObject> files = null;
        try {
            files = downloader.getContentRoot(directoryRootUrl);
        } catch (IOException e) {
            return null;
        }

        List<Question> questions = files.parallelStream()
                .map(downloader::parseQuestion)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ret.setQuestions(questions);

        try {
            downloader.getMetadata(ret, directoryRootUrl);
        } catch (IOException e) {
            log.error("Could not fetch metadata for test: " + id);
        }

        log.info("Test '" + id + "' parsed");

        return ret;
    }






//    /**
//     * Performs http GET request to specified url
//     * @param url valid url
//     * @return String representation of response body
//     * @throws IOException if there is any problem with connection
//     * @throws IllegalStateException when response status is other than 200
//     */
//    @Deprecated
//    public String getStringFromUrl(String url) throws IOException{
//
//        StringBuilder bodyBuilder = new StringBuilder();
//
//        HttpUriRequest request = RequestBuilder.get(url).addParameter("access_token", oauthToken).build();
//
//        HttpResponse response = getHttpClient().execute(request);
//
//        if (response.getStatusLine().getStatusCode() != 200) {
//            System.out.println(request.getRequestLine());
//            throw new IllegalStateException("Http response status: " + response.getStatusLine());
//        }
//        reading response
//        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
//        String line;
//        while((line = br.readLine()) != null) {
//            bodyBuilder.append(line).append('\n');
//        }
//
//        return bodyBuilder.toString();
//    }

}
