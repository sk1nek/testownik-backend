package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NonNull;

@Data
@AllArgsConstructor
public class Answer {

    @NonNull
    private boolean isCorrect;

    private boolean hasGraphic;

    @NonNull
    private String text;

    private String graphicUrl;

}