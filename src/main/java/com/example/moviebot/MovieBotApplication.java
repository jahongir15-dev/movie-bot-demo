package com.example.moviebot;

import com.example.moviebot.repository.VideosRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

@SpringBootApplication
public class MovieBotApplication {

    private static VideosRepository videosRepository;

    public MovieBotApplication(VideosRepository videosRepository) {
        MovieBotApplication.videosRepository = videosRepository;
    }

    public static void main(String[] args) {
        try {
            SpringApplication.run(MovieBotApplication.class, args);
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new TelegramBot(videosRepository));
        } catch (TelegramApiException err) {
            System.err.println("Telegram API exception: " + err.getMessage());
        }
    }
}
