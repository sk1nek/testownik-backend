package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Question {

    private String header;

    private long questionNumber;

    private List<Answer> answers;

}
