package me.mjaroszewicz.controllers;

import me.mjaroszewicz.entities.Test;
import me.mjaroszewicz.storage.EntityNotFoundException;
import me.mjaroszewicz.storage.TestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class MainController {

    @Autowired
    private TestRepository testRepository;

    @GetMapping("/tests/{id}")
    public ResponseEntity<Test> getTestById(@PathVariable String id){

        Test test;
        try {
            test = testRepository.get(id);
        } catch (EntityNotFoundException e) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(test);
    }

}
