package me.mjaroszewicz.service;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.mjaroszewicz.entities.Answer;
import me.mjaroszewicz.entities.Question;
import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.TestRepository;
import org.asynchttpclient.AsyncHttpClient;
import org.asynchttpclient.Dsl;
import org.asynchttpclient.Request;
import org.asynchttpclient.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@EnableScheduling
public class GithubDownloader {

    private static final Logger log = LoggerFactory.getLogger(GithubDownloader.class);

    @Value("${github.oauth.token}")
    private String oauthToken;

    @Value("${http.user.agent}")
    private String userAgent;

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.raw.url}")
    private String rawUrl;

    //pattern matching content between [img] tags
    private final static Pattern imgPattern = Pattern.compile("\\[img].*\\[/img]");

    @Autowired
    private TestRepository testRepository;

    Mono<Response> get(String url){

        AsyncHttpClient client = Dsl.asyncHttpClient();

            Request request = Dsl
                    .get(url)
                    .addQueryParam("access_token", oauthToken)
                    .setFollowRedirect(true)
                    .build();

            return Mono.fromFuture(client.executeRequest(request).toCompletableFuture());

    }

    /**
     * Starts asynchronous repository fetching process
     * @param url - contents url
     */
    void go(String url){

       get(url).subscribe(p-> {

           long start = System.currentTimeMillis();

           log.info("Repository downloaded. ");
           try {
               Files.write(Paths.get("repo.zip"), p.getResponseBodyAsBytes());
               log.info("Repository successfully saved. ");

           } catch (IOException e) {
               e.printStackTrace();
               return;
           }

           FileUtils.unzip(Paths.get("repo.zip"), Paths.get("git"));
           log.info("Repository unzipped in " + (System.currentTimeMillis() - start) + "ms");

           parseRepository();
       });

    }

    /**
     *
     */
    private void parseRepository(){

        log.info("Beginning repository parse.");

        Path root = Paths.get("git/testownik-baza-master");
        File[] files = root.toFile().listFiles();

        log.info("Found " + files.length + " test directories. ");

        for (File f : files){
            try{
                parseTest(f);
            }catch (IOException ioex){
                log.warn("IOException while parsing '" + f.getName() + "': " + ioex.getMessage());
            }
        }
    }

    /**
     * Creates test from provided directory and forwards it to repository.
     * @param directory directory containing test
     */
    private void parseTest(File directory) throws IOException{

        log.info("Parsing " + directory.getName());

        Test test = new Test();
        test.setId(directory.getName());

        Gson gson = new Gson();

        String metadata = new String(Files.readAllBytes(directory.toPath().resolve("test.md")));
        JsonObject obj = gson.fromJson(metadata, JsonObject.class);

        String title = obj.get("title").getAsString();
        String desc = obj.get("description").getAsString();

        test.setTitle(title);
        test.setDescription(desc);

        List<Question> questions =
                Arrays.stream(directory.listFiles())
                        .filter(p -> p.getName().contains(".txt"))
                        .map(p -> questionFromFile(p, test.getId()))
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        test.setQuestions(questions);

        testRepository.add(test);
    }

    /**
     * Parses question from file. If doing batch operation, results need to be filtered for null occurences.
     *
     * @param f file containing question data
     * @param testId test id String
     * @return valid question on success, null on IOException during read or when name equals 'test.md'
     */
    private Question questionFromFile(File f, String testId){

        if(f.getName().equals("test.md"))
            return null;

        byte[] bytes;

        try{
            bytes = Files.readAllBytes(f.toPath());
        }catch (IOException ex){
            log.warn("Could not read question file: " + f.getPath() + " - " + ex.getMessage());
            return null;
        }

        String fileContent = new String(bytes, Charset.forName("Windows-1250")); //file content

        List<String> lines = Arrays.asList(fileContent.split("\n"));

        Set<Integer> correct = new HashSet<>();
        String firstLine = lines.get(0).substring(1);
        //find correct answer numbers
        for(int i = 0; i < firstLine.length(); i++)
            if(firstLine.charAt(i) == '1')
                correct.add(i);

        String header = lines.get(1);

        //parsing header
        Matcher headerMatcher = imgPattern.matcher(header);

        if(headerMatcher.find()){
            String match = headerMatcher.group(0);
            header = rawUrl + testId + "/" + match.substring(5, match.length() - 6);
        }

        List<Answer> answers = new ArrayList<>();
        for(int i = 2; i < lines.size(); i++){

            String content = lines.get(i);

            Matcher answerMatcher = imgPattern.matcher(content);

            if (answerMatcher.find()) {
                String match = answerMatcher.group(0);
                content = rawUrl + testId + "/" + match.substring(5, match.length() - 6);
            }

            answers.add(new Answer(correct.contains(i - 2), content));
        }

        return new Question(header, answers);
    }

}
