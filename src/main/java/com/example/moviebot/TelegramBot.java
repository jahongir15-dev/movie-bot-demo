package com.example.moviebot;

import com.example.moviebot.entity.Videos;
import com.example.moviebot.repository.VideosRepository;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.*;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.File;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    Map<Long, String> info = new HashMap<>();
    Map<Long, String> infVid = new HashMap<>();
    Map<Long, String> name = new HashMap<>();
    Map<Long, String> code = new HashMap<>();
    Map<Long, InputFile> vid = new HashMap<>();
    Set<Videos> videos = new HashSet<>();


    private final VideosRepository videosRepository;

    public TelegramBot(VideosRepository videosRepository) {
        this.videosRepository = videosRepository;
    }


    @Override
    public String getBotUsername() {
        return "movie_demo_uz_bot";
    }

    @Override
    public String getBotToken() {
        return "6923550332:AAH2yu-lqiEyqqML_3tKfl7PtX2g3YnASp0";
    }

    private static final long ADMIN_CHAT_ID = 5699941692L;
    private static final String CHANNEL_USERNAME = "@test_channel_demo";

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            String text = message.getText();
            if (userId == ADMIN_CHAT_ID) {
                adminCommand(chatId, text, message);
            } else {
                userCommand(chatId, text, userId);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            Long chatId = callbackQuery.getFrom().getId();
            Integer messageId = update.getMessage().getMessageId();
            long userId = update.getCallbackQuery().getFrom().getId();
            if (data.equals("Tasdiqlash")) {
                boolean isSubscribed = checkSubscription(userId);
                if (isSubscribed) {
                    execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                    sendMenuKeyboard(chatId);
                } else {
                    try {
                        execute(AnswerCallbackQuery.builder().callbackQueryId(callbackQuery.getId()).text("Siz hali obuna bo'lmadingiz!").build());
                    } catch (TelegramApiException exception) {
                        exception.printStackTrace();
                    }
                }
            }
        }
    }


    public void userCommand(Long chatId, String text, Long userId) {
        boolean isSubscribed = checkSubscription(userId);
        if (text.equals("/start")) {
            if (isSubscribed) {
                sendMenuKeyboard(chatId);
            } else {
                sendInlineKeyboard(chatId);
            }
        } else if (text.equals("Filmlar")) {
            File file;
            InputFile inputFile;
            file = new File("C:\\Users\\jahon\\Downloads\\video_2024-01-19_01-03-40.mp4");
            inputFile = new InputFile(file);
            try {
                execute(SendVideo.builder().chatId(chatId).video(inputFile).caption("???").build());
            } catch (TelegramApiException exception) {
                exception.printStackTrace();
            }
        } else if (text.equals("Film izlash")) {
            sendTextMessage(chatId, "Film kodini kiriting");
        }
    }

    public void adminCommand(Long chatId, String text, Message message) {
        if (text.equals("/start")) {
            sendAdminMenuKeyboard(chatId);
        } else if (text.equals("Kodli film qo'shish")) {
            sendTextMessage(chatId, "Film nomi kiriting");
            info.put(chatId, "movie name");
        } else if (info.size() > 0) {
            addMovieCode(chatId, text, message);
        }
    }

    private boolean checkSubscription(long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember(CHANNEL_USERNAME, userId);
            ChatMember chatMember = execute(getChatMember);
            return chatMember.getStatus().equals("member") || chatMember.getStatus().equals("administrator");

        } catch (TelegramApiException e) {
            System.err.println("user not subscribed channel");
            return false;
        }
    }

    private void sendInlineKeyboard(Long chatId) {
        InlineKeyboardMarkup inlineKeyboardMarkup = Channel();
        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId);
        sendMessage.setText("Botimizdan to'liq foydalanish uchun telegram kanalga obuna bo'ling va Tekshirish tugmasini bosing.");
        sendMessage.setReplyMarkup(inlineKeyboardMarkup);

        try {
            execute(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    public void sendTextMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            System.err.println("Not Text");
        }
    }

    public InlineKeyboardMarkup Channel() {
        List<List<InlineKeyboardButton>> lists = new ArrayList<>();
        List<InlineKeyboardButton> link = new ArrayList<>();
        List<InlineKeyboardButton> checkBtn = new ArrayList<>();
        InlineKeyboardButton linkButton = new InlineKeyboardButton();
        InlineKeyboardButton check = new InlineKeyboardButton();

        linkButton.setText("Kanal");
        linkButton.setUrl("https://t.me/test_channel_demo");

        check.setCallbackData("Tasdiqlash");
        check.setText("Tasdiqlash ✅");

        link.add(linkButton);
        checkBtn.add(check);

        lists.add(link);
        lists.add(checkBtn);
        InlineKeyboardMarkup markup = new InlineKeyboardMarkup();
        markup.setKeyboard(lists);
        return markup;
    }

    private void sendMenuKeyboard(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(false);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardButtonsRow1 = new KeyboardRow();
        keyboardButtonsRow1.add(new KeyboardButton("Filmlar"));
        keyboardButtonsRow1.add(new KeyboardButton("Film izlash"));

        keyboard.add(keyboardButtonsRow1);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Bo'limni tanlang.");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void sendAdminMenuKeyboard(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(false);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardButtonsRow1 = new KeyboardRow();
        keyboardButtonsRow1.add(new KeyboardButton("Film qo'shish"));
        keyboardButtonsRow1.add(new KeyboardButton("Kodli film qo'shish"));

        keyboard.add(keyboardButtonsRow1);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Assalomu alaykum admin!");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void addMovieCode(Long chatId, String text, Message message) {
        try {
            if (info.get(chatId).equals("movie name")) {
                sendTextMessage(chatId, "Film kodini kiriting");
                info.remove(chatId);
                name.put(chatId, text);
                info.put(chatId, "code");
            } else if (info.get(chatId).equals("code")) {
                sendTextMessage(chatId, "Film yuboring");
                info.remove(chatId);
                code.put(chatId, text);
                info.put(chatId, "video");
            } else if (info.get(chatId).equals("video")) {
                sendTextMessage(chatId, "Film saqlandi");
                if (message.hasDocument()) {
                    Document document = message.getDocument();
                    String fileId = document.getFileId();
                    InputFile inputFile = new InputFile(fileId);
                    vid.put(chatId, inputFile);

                    Videos build = Videos.builder()
                            .name(name.get(chatId))
                            .code(code.get(chatId))
//                            .video(vid.get(chatId))
                            .build();
                    videosRepository.save(build);

                    info.remove(chatId);
                } else {
                    sendTextMessage(chatId, "Invalid file format. Please send a valid video file.");
                }
            }
        } catch (Exception e) {
            e.printStackTrace(); // Handle the exception appropriately (log it, send a message, etc.)
        }
    }

}
