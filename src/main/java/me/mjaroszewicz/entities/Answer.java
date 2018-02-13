package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Answer {

    @NonNull
    private boolean isCorrect;

    @NonNull
    private String text;


}