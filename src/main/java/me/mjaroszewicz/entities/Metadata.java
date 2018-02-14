package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Metadata {

    private String title;

    private String id;

    private String description;
}
