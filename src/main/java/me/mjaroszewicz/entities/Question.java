package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class Question implements Comparable<Question>{

    private String header;

    private long questionNumber;

    private List<Answer> answers;

    @Override
    public int compareTo(Question question) {

        return (int) Math.signum(questionNumber - question.getQuestionNumber());

    }
}
