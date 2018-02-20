package me.mjaroszewicz.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.mjaroszewicz.entities.Answer;
import me.mjaroszewicz.entities.Question;
import me.mjaroszewicz.entities.Test;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class HttpDownloader {

    private HttpClient httpClient;

    @Value("${github.oauth.token}")
    private String oauthToken;

    @Value("${http.user.agent}")
    private String userAgent;

    /**
     * Basic constructor. It instantiates HttpClient
     */
    public HttpDownloader(){
        httpClient = HttpClientBuilder.create().build();
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
        HttpResponse response = getUrl(url);

        //reading response
        BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line;
        while((line = br.readLine()) != null) {
            bodyBuilder.append(line).append('\n');
        }

        return bodyBuilder.toString();
    }

    public HttpResponse getUrl(String url) throws IOException{

        HttpUriRequest request = RequestBuilder
                .get(url)
                .addParameter("access_token", oauthToken)
                .build();

        HttpResponse response = httpClient.execute(request);

        if(response.getStatusLine().getStatusCode() != 200)
            throw new IllegalStateException("Http response status: " + response.getStatusLine());

        return response;
    }

    void getMetadata(Test t, String dirRoot) throws IOException{

        Gson gson = new Gson();

        System.out.println(dirRoot);
        System.out.println(dirRoot.replaceFirst("\\?ref=master", "/test.md"));
        String response = getStringFromUrl(dirRoot.replaceFirst("\\?ref=master", "/test.md"));

        System.out.println(response);

        JsonObject metadata = gson.fromJson(new String(Base64.getMimeDecoder().decode(response)), JsonObject.class);

        t.setTitle(metadata.get("title").getAsString());
        t.setDescription(metadata.get("description").getAsString());

    }

    /**
     *
     * @param file
     * @return null if http request was unsuccessful
     */
    Question parseQuestion(JsonObject file) {

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
    List<JsonObject> getContentRoot(String contentsUrl) throws IOException{

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


}
