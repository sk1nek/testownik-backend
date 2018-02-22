package me.mjaroszewicz.entities;

import lombok.*;

import java.util.List;

/**
 * Entity containing basic information about test and associated questions/
 */
@Data
@NoArgsConstructor
@RequiredArgsConstructor
@AllArgsConstructor
public class Test {

    @NonNull
    private String title;

    @NonNull
    private String id;

    @NonNull
    private String description;

    @NonNull
    private List<Question> questions;

    private String url;

    public Metadata metadata(){
        return new Metadata(title, id ,description);
    }

}
