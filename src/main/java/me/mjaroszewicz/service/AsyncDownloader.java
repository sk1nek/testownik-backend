package me.mjaroszewicz.service;

import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.TestRepository;
import org.asynchttpclient.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;

@Component
@EnableScheduling
public class AsyncDownloader {

    @Value("${github.oauth.token}")
    private String oauthToken;

    @Value("${http.user.agent}")
    private String userAgent;

    private Flux<Test> testFlux;

    private String rootUrl;

    @Autowired
    private TestRepository testRepository;


    public Mono<Response> get(String url){

        try(AsyncHttpClient client = Dsl.asyncHttpClient()){

            Request request = Dsl
                    .get(url)
                    .addQueryParam("access_token", oauthToken)
                    .build();

            return Mono.fromFuture(client.executeRequest(request).toCompletableFuture());

        }catch (Exception ex){
            return Mono.error(ex);
        }

    }

    public void go(String url){



    }








}

//    List<JsonObject> getContentRoot(String contentsUrl) throws IOException {
//
//        Gson gson = new Gson();
//
//        String body = getStringFromUrl(contentsUrl);
//
//        JsonObject[] strings = null;
//        try{
//            strings = gson.fromJson(body, JsonObject[].class);
//        }catch(Throwable t){
//            System.out.println(body);
//        }
//
//        List<JsonObject> ret = new ArrayList<>();
//
//        for (JsonObject string : strings) {
//            ret.add(string.getAsJsonObject());
//        }
//
//        return ret;
//    }
//}

//    /**
//     *
//     * @param dir
//     * @return null if http request was unsuccessful
//     */
//    private Test testFromDirectory(JsonObject dir)  {
//
//        HttpDownloader downloader = new HttpDownloader();
//
//        String defaultDescription = "Default description";
//        String id = dir.get("path").getAsString();
//        String defaultTitle = id;
//
//        Test ret = new Test(defaultTitle, id, defaultDescription, Collections.emptyList());
//
//        String directoryRootUrl = dir.get("url").getAsString();
//
//        List<JsonObject> files = null;
//        try {
//            files = downloader.getContentRoot(directoryRootUrl);
//        } catch (IOException e) {
//            return null;
//        }
//
//        List<Question> questions = files.parallelStream()
//                .map(downloader::parseQuestion)
//                .filter(Objects::nonNull)
//                .collect(Collectors.toList());
//
//        ret.setQuestions(questions);
//
//        try {
//            downloader.getMetadata(ret, directoryRootUrl);
//        } catch (IOException e) {
//            log.error("Could not fetch metadata for test: " + id);
//        }
//
//        log.info("Test '" + id + "' parsed");
//
//        return ret;
//    }
