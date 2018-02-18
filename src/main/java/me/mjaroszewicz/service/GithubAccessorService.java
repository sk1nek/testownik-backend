package me.mjaroszewicz.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.mjaroszewicz.entities.Answer;
import me.mjaroszewicz.entities.Question;
import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.TestRepository;
import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Service;
import org.springframework.util.Base64Utils;
import sun.nio.cs.ext.ExtendedCharsets;

import javax.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@SuppressWarnings("Duplicates")
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

    @Autowired
    private TestRepository repository;

    @Bean
    HttpClient getHttpClient(){

        HttpClientBuilder builder = HttpClientBuilder.create();
        builder
                .setUserAgent(userAgent);

        return builder.build();

    }

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
        httpClient = getHttpClient();

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

        getContentRoot(contentsUrl).parallelStream()
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

        String defaultDescription = "Default description";
        String defaultTitle = "Title";
        String id = dir.get("path").getAsString();

        Test ret = new Test(defaultTitle, id, defaultDescription, Collections.emptyList());

        String directoryRootUrl = dir.get("url").getAsString();

        List<JsonObject> files = null;
        try {
            files = getContentRoot(directoryRootUrl);
        } catch (IOException e) {
            return null;
        }

        List<Question> questions = files.stream()
                .map(this::parseQuestion)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        ret.setQuestions(questions);

        return ret;
    }

    /**
     *
     * @param file
     * @return null if http request was unsuccessful
     */
    private Question parseQuestion(JsonObject file) {

        Gson gson = new Gson();

        String url = file.get("url").getAsString();

        String questionString = null;
        try {
            questionString = getStringFromUrl(url);
        } catch (IOException e) {
            return null;
        }
        JsonObject questionJson = gson.fromJson(questionString, JsonObject.class);

        //base64 encoded file content
        String contentEncoded = questionJson.get("content").getAsString();

        //decode

        String content = new String(Base64.getMimeDecoder().decode(contentEncoded));
        String[] split = content.split("\n");

        int correctPos = split[0].indexOf(1); //correct question position
        String header = split[1];

        List<Answer> answers = new ArrayList<>();

        for(int i = 2 ; i < split.length; i++)
            answers.add(new Answer(i == correctPos - 2, split[i]));


        return new Question(header, answers);
    }

    /**
     *
     * @param contentsUrl url to github api pointing at root of contents, ex. 'https://api.github.com/repos/user/repo_name/contents/'
     * @return Repository content root represented as list of json objects
     * @throws IOException
     */
    private List<JsonObject> getContentRoot(String contentsUrl) throws IOException{

        Gson gson = new Gson();

        String body = getStringFromUrl(contentsUrl);

        JsonObject[] strings = null;
        try{

            strings = gson.fromJson(body, JsonObject[].class);
        }catch(Throwable t){
            System.out.println(body);
        }

        List<JsonObject> ret = new ArrayList<>();

        for (JsonObject string : strings) {
            ret.add(string.getAsJsonObject());
        }

        return ret;
    }

    /**
     * Performs http GET request to specified url
     * @param url valid url
     * @return String representation of response body
     * @throws IOException if there is any problem with connection
     * @throws IllegalStateException when response status is other than 200
     */
    public String getStringFromUrl(String url) throws IOException{

        StringBuilder bodyBuilder = new StringBuilder();

        HttpUriRequest request = RequestBuilder.get(url).addParameter("access_token", oauthToken).build();

        HttpResponse response = getHttpClient().execute(request);

        if (response.getStatusLine().getStatusCode() != 200) {
            System.out.println(request.getRequestLine());
            throw new IllegalStateException("Http response status: " + response.getStatusLine());
        }
        //reading response
        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        while((line = br.readLine()) != null) {
            bodyBuilder.append(line).append('\n');
        }

        return bodyBuilder.toString();
    }

}
