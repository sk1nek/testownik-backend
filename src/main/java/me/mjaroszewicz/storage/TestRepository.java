package me.mjaroszewicz.storage;

import me.mjaroszewicz.entities.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

@Repository
public class TestRepository {

    private final static Logger log = LoggerFactory.getLogger(TestRepository.class);

    private HashMap<String, Test> testHolder;

    private ArrayList<String> keysArray;

    private static Random generator;

    @PostConstruct
    private void init(){
        log.debug("Initializing Test Repository. ");
        testHolder = new HashMap<>();
        keysArray = new ArrayList<>();
        generator = new Random();
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
     * Removes Test identified by provided String
     * @param id String representation of id associated with Test object
     */
    public void remove(String id) throws EntityNotFoundException{

        if(!keysArray.contains(id))
            throw new EntityNotFoundException("Test with id '" + id + "' not found. ");

        testHolder.remove(id);
        keysArray.remove(id);

    }



}