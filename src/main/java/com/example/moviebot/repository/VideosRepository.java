package com.example.moviebot.repository;

import com.example.moviebot.entity.Videos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VideosRepository extends JpaRepository<Videos, Long> {
    Optional<Videos> findVideosByName(String name);
}
