package me.mjaroszewicz.controllers;

import me.mjaroszewicz.entities.Metadata;
import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.EntityNotFoundException;
import me.mjaroszewicz.storage.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/api")
public class MainController {

    @Autowired
    private TestRepository testRepository;

    private MultiValueMap<String, String> getHeaders(){
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        headers.add("Access-Control-Allow-Origin", "*");

        return headers;
    }

    @GetMapping("/tests")
    public ResponseEntity<List<Metadata>> getAllTestsMetadata(){

        List<Metadata> metadataList = testRepository.getAllMetadata();


        return new ResponseEntity<>(metadataList, getHeaders(), HttpStatus.OK);
    }

    @GetMapping("/tests/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable String id){

        Test test;
        try {
            test = testRepository.get(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return new ResponseEntity<>(test, getHeaders(), HttpStatus.OK);
    }

    @GetMapping("/tests/random")
    public ResponseEntity<Test> getRandomTest(){

        Test test;
        try {
            test = testRepository.getRandomTest();
        } catch (EntityNotFoundException e) {
            e.printStackTrace();
            return ResponseEntity.noContent().build();
        }

        return new ResponseEntity<>(test, getHeaders(), HttpStatus.OK);
    }

}
