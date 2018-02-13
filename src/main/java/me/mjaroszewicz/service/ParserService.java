package me.mjaroszewicz.service;

import me.mjaroszewicz.entities.Answer;
import me.mjaroszewicz.entities.Question;
import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.TestRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ParserService {

    private Pattern answersPattern = Pattern.compile("(X(\\d{2,}))");

    private final static Logger log = LoggerFactory.getLogger(ParserService.class);

    @Value("${github.cdn.url}")
    private String cdnUrl;


    @Autowired
    private TestRepository testRepository;

    /**
     * Simple initialization method - checks if cdn url (property 'github.cdn.url') is blank
     * @throws IllegalStateException if cdn url is null/blank
     */
    @PostConstruct
    private void init() throws AssertionError{
        log.info("Initializing Parser service. ");

        if(cdnUrl == null || cdnUrl.isEmpty()) {
            log.error("Fatal error : CDN url empty or null. )");
            throw new AssertionError();
        }

    }

    /**
     * Parses provided body, searches it for Test directories, parses all of them and forwards collection to TestRepository
     * @param body
     */
    public void parseRepositoryData(String body){

        Document doc = Jsoup.parse(body);

        List<Pair> unparsedTests = getDirectoryUrlList(doc);

        //will count parsing fails

        //using parallel stream hoping it will be faster with bigger repos
        List<Test> tests = unparsedTests
                .parallelStream()
                .flatMap((Pair p) -> {
                    try {
                        return Stream.of(fetchTest(p));
                    } catch (Throwable t) {
                        t.printStackTrace();
                        log.error("Failed processing of test: " + p.id);
                    }
                    return Stream.empty();
                })
                .collect(Collectors.toList());

        testRepository.addAll(tests);

    }

    /**
     * Parses github page in search of directory hrefs
     *
     * @param doc github page html body containing files table
     * @return list of Pair class members containing directory id and its href
     */
    private List<Pair> getDirectoryUrlList(Document doc){

        ArrayList<Pair> ret = new ArrayList<>();

        //getting table body
        Element filesTable = doc.body()
                .getElementsByClass("files").get(0)
                .getElementsByTag("tbody").get(0);

        Elements rows = filesTable.getElementsByClass("js-navigation-item");

        rows.forEach(p->{
            for (Element href : p.getElementsByAttribute("href")) {
                if(href.hasClass("js-navigation-open")) {

                    if(href.html().equals("metadata"))
                        continue;

                    Pair pair = new Pair(href.html(), href.attr("href"))
                    ret.add(pair);
                }
            }
        });



        return ret;
    }

    /**
     *
     * @param p pair consisting of directory id and partial url
     * @return complete Test object
     */
    private Test fetchTest(Pair p) throws IOException {

        log.info("Fetching test '" + p.id + "'");

        HttpDownloader downloader = new HttpDownloader();

        String url = "https://github.com" + p.href;

        String body = downloader.getStringFromUrl(url);

        Document doc = Jsoup.parse(body);

        List<Question> questions = new ArrayList<>();

        doc.select("span.css-truncate.css-truncate-target")
                .stream()
                .map(a -> a.getElementsByAttribute("href"))
                .filter(a -> a.text().contains("txt") || a.text().contains("png"))
                .filter(a -> !a.isEmpty())
                .flatMap(a -> Stream.of(a.first()))
                .filter(a -> a.text().contains(".txt"))
                .forEach(a -> {
                    try{
                        questions.add(questionFromHref(a, p.id));
                    }catch(Throwable t){
                        t.printStackTrace();
                        log.error("Unable to get question from file: " + a.text());
                    }
                });

        Test test = new Test();
        test.setDescription("Test"); //todo
        test.setId(p.id);
        test.setQuestions(questions);
        test.setTitle(p.id); //todo

        log.info("Finished");

        return test;
    }


    /**Function that processes 'a' html tag into question. Uses HttpDownloader so it may have some impact on performance.
     *
     * @param a html 'a' tag containing href to question resource
     * @return newly built question
     */
    private Question questionFromHref(Element a, String directoryName) throws IOException {


        String name = a.text();


        StringBuilder urlBuilder = new StringBuilder(cdnUrl);
        urlBuilder.append(directoryName).append('/').append(name);

        HttpDownloader downloader = new HttpDownloader();
        String response = downloader.getStringFromUrl(urlBuilder.toString());

        String[] split = response.split("\n");

        int correctAnswerPosition = extractCorrectAnswerPosition(split[0]);
        String questionHeader = parseImageString(split[1], directoryName);

        List<Answer> answers = new ArrayList<>();

        for (int i = 2; i < split.length; i++) {

            boolean correct = i - 2 == correctAnswerPosition;

            Answer answer = new Answer(correct, parseImageString(split[i], directoryName));
            answers.add(answer);

        }

        Question question = new Question(questionHeader, Long.parseLong(name.substring(0, 3)), answers);

        return question;
    }

    private String parseImageString(String s, String dir){

        if(!s.contains("img"))
            return s;
        String img = s.substring(5, s.length() - 6);


        return getCdnUrl(img, dir);
    }

    /**
     * @param name name of the file
     * @param dir test id
     * @return url to specified file on github content delivery network
     */
    private String getCdnUrl(String name, String dir){

        StringBuilder sb = new StringBuilder(cdnUrl);
        sb.append(dir).append('/').append(name);

        return sb.toString();
    }

    /**
     * Finds encoded answers matching pattern and extracts position of first '1' from result string. For example: when answers are written as X0001, this function returns 3.
     * @param string string to be searched
     * @return position of first matching '1', -1 when nothing could be found;
     */
    private int extractCorrectAnswerPosition(String string){
        Matcher m = answersPattern.matcher(string);

        //preventing IllegalStateException
        if(m.find()){
            String group = m.group();
            for(int i = 1; i < group.length(); i++)
                if(group.charAt(i) == '1')
                    return i - 1;
        }

        return -1;
    }



    class Pair{

        String id;
        String href;

        Pair(String id, String href){
            this.id = id;
            this.href = href;
        }

    }

}
