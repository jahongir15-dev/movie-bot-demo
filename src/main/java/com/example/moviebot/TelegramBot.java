package com.example.moviebot;

import com.example.moviebot.entity.Videos;
import com.example.moviebot.repository.VideosRepository;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendDocument;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
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

import java.io.*;
import java.net.URL;
import java.util.*;

public class TelegramBot extends TelegramLongPollingBot {

    private static final long MAX_ALLOWED_FILE_SIZE = 1024L * 1024L * 1024L; // 1 GB

    Map<Long, String> info = new HashMap<>();
    Map<Long, Video> infVid = new HashMap<>();
    Map<Long, String> name = new HashMap<>();
    Map<Long, String> code = new HashMap<>();
    Map<Long, Video> vid = new HashMap<>();

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
            long chatId = message.getChatId();
            long userId = message.getFrom().getId();
            String text = message.getText();
            if (userId == ADMIN_CHAT_ID) {
                adminCommand(chatId, text, message);
            } else {
                userCommand(chatId, text, userId);
            }
        } else if (update.hasMessage() && update.getMessage().hasVideo()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            long userId = message.getFrom().getId();
            if (userId == ADMIN_CHAT_ID) {
                adminCommand(chatId, "Video received", message);
            } else {
                userCommand(chatId, message.getText(), userId);
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            Long chatId = callbackQuery.getFrom().getId();
            Integer messageId = callbackQuery.getMessage().getMessageId();
            long userId = callbackQuery.getFrom().getId();
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
            }else if (isSubscribed){
                if (text.equals("Filmlar")) {
                    sendTextMessage(chatId, "Hozircha filmlar mavjud emas");
                } else if (text.equals("Film izlash")) {
                    sendTextMessage(chatId, "Film kodini kiriting");
                    info.put(chatId, "movie code");
                } else if (info.size() > 0) {
                    getCodeMovie(chatId, text);
                }
            }else {
                sendInlineKeyboard(chatId);
            }
        }

    public void adminCommand(Long chatId, String text, Message message) {
        if (text.equals("/start")) {
            sendAdminMenuKeyboard(chatId);
        } else if (text.equals("Kodli film qo'shish 🗝️")) {
            sendTextMessage(chatId, "Film nomi kiriting");
            info.put(chatId, "movie name");
        } else if (info.size() > 0) {
            addMovieCode(chatId, message);
        } else if (text.equals("Reklama joylash 📢")) {
            sendTextMessage(chatId, "Reklamani menga yuboring");
            postAdvertising(message, chatId, text);
        }
    }

    private void postAdvertising(Message message, Long chatId, String text) {
        if (message.hasDocument() || message.hasPhoto() || message.hasText()) {
            if (message.hasDocument()) {
                Document document = message.getDocument();
                String fileId = document.getFileId();
                InputFile inputFile = new InputFile(fileId);
                sendTextMessage(chatId, "Reklama joylandi");
                try {
                    execute(SendDocument.builder().chatId(ADMIN_CHAT_ID).document(inputFile).caption("#vacation\n\nMa'lumot: " + info.get(chatId) + "\n\n👤 Yuboruvchi: @" + message.getFrom().getUserName()).build());
                } catch (TelegramApiException exception) {
                    exception.printStackTrace();
                }
            } else if (message.hasPhoto()) {
                try {
                    sendTextMessage(chatId, "Reklama joylandi");
                    PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
                    String fileId = photo.getFileId();
                    InputFile photoInputFile = new InputFile(fileId);
                    execute(SendPhoto.builder()
                            .chatId(String.valueOf(ADMIN_CHAT_ID))
                            .photo(photoInputFile)
                            .build());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        } else {
            sendTextMessage(chatId, "Iltimos fayl yuboring");
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
        KeyboardRow keyboardButtonsRow2 = new KeyboardRow();

        keyboardButtonsRow1.add(new KeyboardButton("Film qo'shish 📽️"));
        keyboardButtonsRow1.add(new KeyboardButton("Kodli film qo'shish 🗝️"));
        keyboardButtonsRow2.add(new KeyboardButton("Statistika 📊"));
        keyboardButtonsRow2.add(new KeyboardButton("Reklama joylash 📢"));

        keyboard.add(keyboardButtonsRow1);
        keyboard.add(keyboardButtonsRow2);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Assalomu alaykum admin !");
        message.setReplyMarkup(keyboardMarkup);

        try {
            execute(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void getCodeMovie(Long chatId, String text) {
        if (info.get(chatId).equals("movie code")) {
            boolean isSubscribed = checkSubscription(chatId);

            if (isSubscribed) {
                Videos videosByCode = videosRepository.getVideosByCode(text);
                if (videosByCode != null && text.equals(videosByCode.getCode())) {
                    byte[] videoData = videosByCode.getVideoData();

                    InputStream inputStream = new ByteArrayInputStream(videoData);
                    InputFile inputFile = new InputFile().setMedia(inputStream, "movie.mp4");

                    try {
                        execute(SendVideo.builder()
                                .chatId(chatId)
                                .video(inputFile)
                                .caption(videosByCode.getName())
                                .build());
                    } catch (TelegramApiException exception) {
                        exception.printStackTrace();
                    }

                    info.remove(chatId);
                    info.clear();
                } else {
                    sendTextMessage(chatId, "😔 Afsuski film topilmadi yoki kod xato bo'lishi mumkin");
                }
            } else {
                sendTextMessage(chatId, "Siz kanalga obuna bo'lmagansiz");
            }
        }
    }

    private void addMovieCode(Long chatId, Message message) {
        try {
            if (info.get(chatId).equals("movie name")) {
                sendTextMessage(chatId, "Film kodini kiriting:");
                info.remove(chatId);
                name.put(chatId, message.getText());
                info.put(chatId, "code");
            } else if (info.get(chatId).equals("code")) {
                info.remove(chatId);
                code.put(chatId, message.getText());
                sendTextMessage(chatId, "Film yuboring:");
                info.put(chatId, "video");
            } else if (info.get(chatId).equals("video")) {
                if (message.getVideo() != null) {
                    sendTextMessage(chatId, "Film saqlanmoqda...");
                    infVid.put(chatId, message.getVideo());

                    Video video = infVid.get(chatId);
                    if (video != null) {
                        if (video.getFileSize() <= MAX_ALLOWED_FILE_SIZE) {
                            String fileId = video.getFileId();

                            byte[] videoBytes = downloadVideoBytes(fileId);

                            vid.put(chatId, video);
                            Videos build = Videos.builder()
                                    .code(code.get(chatId))
                                    .name(name.get(chatId))
                                    .videoData(videoBytes)
                                    .build();
                            videosRepository.save(build);
                            sendTextMessage(chatId, "Film saqlandi");

                            infVid.remove(chatId);
                            infVid.clear();
                            info.clear();
                        } else {
                            sendTextMessage(chatId, "Video size exceeds the allowed limit.");
                        }
                    } else {
                        sendTextMessage(chatId, "Video not found");
                    }
                } else {
                    sendTextMessage(chatId, "Video not found");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private byte[] downloadVideoBytes(String fileId) throws TelegramApiException, IOException {
        GetFile getFileMethod = new GetFile();
        getFileMethod.setFileId(fileId);
        org.telegram.telegrambots.meta.api.objects.File videoFile = execute(getFileMethod);

        try (InputStream inputStream = new URL(videoFile.getFileUrl(getBotToken())).openStream()) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                byteArrayOutputStream.write(buffer, 0, bytesRead);
            }
            return byteArrayOutputStream.toByteArray();
        }
    }
}
