package me.mjaroszewicz.service;

import me.mjaroszewicz.storage.TestRepository;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Service
public class ParserService {

    private final static Logger log = LoggerFactory.getLogger(ParserService.class);

    @Autowired
    private TestRepository testRepository;

    @PostConstruct
    private void init(){
        log.info("Initializing Parser service. ");


    }

    /**
     * Parses provided body, searches it for Test directories, parses all of them and forwards collection to TestRepository
     * @param body
     */
    public void parseRepositoryData(String body){

        Document doc = Jsoup.parse(body);

        List<Pair> unparsedTests = getDirectoryUrlList(doc);

        unparsedTests.forEach(p -> System.out.println(p.href + " " + p.id));



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
                if(href.hasClass("js-navigation-open"))
                    ret.add(new Pair(href.attr("href"), href.html()));
            }
        });


        return ret;
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
