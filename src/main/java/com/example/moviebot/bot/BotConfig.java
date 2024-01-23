package com.example.moviebot.bot;

import org.telegram.telegrambots.meta.api.objects.Video;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public interface BotConfig {
    String BOT_USERNAME = "movie_demo_uz_bot";

    String BOT_TOKEN = "6923550332:AAH2yu-lqiEyqqML_3tKfl7PtX2g3YnASp0";

    Set<Long> IS_USERS = new HashSet<>();

    Map<Long, String> IS_MOVIE_INF = new HashMap<>();

    Map<Long, String> IS_NAME = new HashMap<>();

    Map<Long, String> IS_COUNTRY = new HashMap<>();

    Map<Long, String> IS_LANGUAGE = new HashMap<>();

    Map<Long, String> IS_QUALITY = new HashMap<>();

    Map<Long, String> IS_GENRE = new HashMap<>();

    Map<Long, String> IS_CODE = new HashMap<>();

    Map<Long, Video> IS_VIDEO = new HashMap<>();

    Map<Long, String> IS_ADD = new HashMap<>();

    Map<Long, Video> IS_VIDEO_INFO = new HashMap<>();
}
