package me.mjaroszewicz.service;

import me.mjaroszewicz.storage.TestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@EnableScheduling
public class RepositoryFetcherService {

    private final static Logger log = LoggerFactory.getLogger(RepositoryFetcherService.class);

    private final Pattern validMetadataLinePattern = Pattern.compile("\\w+&\\w+&.+");

    @Autowired
    private ParserService parserService;

    @Autowired
    private TestRepository testRepository;

    @Autowired
    private HttpDownloader httpDownloader;

    @Value("${github.repo.url}")
    private String repoUrl;

    @Value("${github.metadata.url}")
    private String metadataUrl;

    @PostConstruct
    private void init() throws AssertionError{

        log.info("Initializing Repository Fetcher Service");
        log.info("GitHub repository address set to: " + repoUrl);

        if(repoUrl == null || !repoUrl.contains("https://github.com/"))
            throw new AssertionError("Provided github repository url does not link to github. ");

        if(metadataUrl == null || metadataUrl.isEmpty() || !metadataUrl.contains("github"))
            log.warn("Possibly blank or invalid metadata, some non-vital info may be unaccessible. " + metadataUrl);
        else
            log.info("Metadata url: " + metadataUrl);
    }

    @Scheduled(fixedDelay = 1000L * 60 * 60)
    private void synchronizeTestRepositories() throws IOException {

        log.info("Beginning repository synchronization.");

        parserService.parseRepositoryData(httpDownloader.getStringFromUrl(repoUrl));

        log.info("Beginning metadata fetch");

        fetchMetaData();

        log.info("Metadata fetched");

    }



    private void fetchMetaData() throws IOException{

        String metadataString = httpDownloader.getStringFromUrl(metadataUrl);

        for (String s : metadataString.split("\n")) {

            //skipping comments
            if (s.charAt(0) == '#')
                continue;

            Matcher m = validMetadataLinePattern.matcher(s);

            if(!m.find())
                continue;

            String[] split = m.group().split("&");

            testRepository.updateMetadata(split[0], split[1], split[2]);

        }

    }










}
