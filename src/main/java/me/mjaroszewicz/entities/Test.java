package me.mjaroszewicz.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Entity containing basic information about test and associated questions/
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Test {

    private String title;

    private String id;

    private String description;

    private List<Question> questions;

    public Metadata metadata(){
        return new Metadata(title, id ,description);
    }

}
