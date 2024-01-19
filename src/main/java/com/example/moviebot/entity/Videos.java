package com.example.moviebot.entity;

import lombok.*;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Video;

import javax.persistence.*;
import java.io.File;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Videos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private String code;

    private InputFile video;
}
