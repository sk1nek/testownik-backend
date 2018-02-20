package me.mjaroszewicz.storage;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import me.mjaroszewicz.entities.Metadata;
import me.mjaroszewicz.entities.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

@Repository
public class TestRepository {

    private final static Logger log = LoggerFactory.getLogger(TestRepository.class);

    private HashMap<String, Test> testHolder;

    private ArrayList<String> keysArray;

    private static Random generator;

    @PostConstruct
    private void init(){
        log.info("Initializing Test Repository. ");

        testHolder = new HashMap<>();
        keysArray = new ArrayList<>();
        generator = new Random();
    }

    public List<Metadata> getAllMetadata(){
        ArrayList<Metadata> ret = new ArrayList<>(testHolder.size());

        testHolder.values().forEach(p -> ret.add(p.metadata()));

        return ret;
    }

    /**
     * Builds Json representation of repository and persists it in file 'db.json'
     */
    private void persist(){

        Gson gson = new Gson();

        JsonArray root = new JsonArray();

        testHolder.forEach((k, v) -> root.add(gson.toJsonTree(v)));

        String content = root.getAsString();

        try {
            Files.write(Paths.get("db.json"), content.getBytes());
        } catch (IOException e) {
            log.error("Error: unable to persist database. ", e);
        }
    }

    /**
     *
     * @return randomly chosen test object
     * @throws EntityNotFoundException if repository is empty
     */
    public Test getRandomTest() throws EntityNotFoundException{

        if(testHolder ==  null || testHolder.isEmpty())
            throw new EntityNotFoundException("There are no tests stored.");

        //no need to use random when there is only one object
        if(testHolder.size() == 1)
            return testHolder.values().iterator().next();

        int position = generator.nextInt(testHolder.size());
        String testId = keysArray.get(position);
        return testHolder.get(testId);

    }

    /**
     * Stores passed test
     * @param t Test class member with non-null id
     */
    public void add(Test t){

        String id = t.getId();
        testHolder.put(id, t);
        keysArray.add(id);

    }

    /**
     * Iterates through provided collections and calls TestRepository#add method on each object
     * @param tests collection containing valid Tests
     */
    public void  addAll(Collection<Test> tests){

        tests.forEach(test -> add(test));

    }

    public Test get(String id) throws EntityNotFoundException{

        if(!keysArray.contains(id))
            throw new EntityNotFoundException("Test not found! ");

        return testHolder.get(id);
    }

    /**
     * Removes Test identified by provided String
     * @param id String representation of id associated with Test object
     */
    public void remove(String id) throws EntityNotFoundException{

        if(!keysArray.contains(id))
            throw new EntityNotFoundException("Test with id '" + id + "' not found. ");

        testHolder.remove(id);
        keysArray.remove(id);

    }

    /**
     * Updates metadata of test
     *
     * @param id Test id
     * @param title New title
     * @param desc New description
     */
    public void updateMetadata(String id, String title, String desc) {

        Test test;

        try {
            test = get(id);
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
            return;
        }

        test.setTitle(title);
        test.setDescription(desc);

        testHolder.put(id, test);

    }



}
