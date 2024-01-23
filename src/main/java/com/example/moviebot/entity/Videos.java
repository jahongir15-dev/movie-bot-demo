package com.example.moviebot.entity;

import lombok.*;

import javax.persistence.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Entity
public class Videos {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String country;

    @Column(nullable = false)
    private String language;

    @Column(nullable = false)
    private String quality;

    @Column(nullable = false)
    private String genre;

    @Column(nullable = false, unique = true)
    private String code;

    private byte[] videoData;
}
