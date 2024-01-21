package com.example.moviebot;

import com.example.moviebot.entity.Videos;
import com.example.moviebot.repository.VideosRepository;
import lombok.SneakyThrows;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
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

    Set<Long> userSet = new HashSet<>();
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
            userSet.add(userId);
            userSet.remove(ADMIN_CHAT_ID);
            String text = message.getText();
            if (userId == ADMIN_CHAT_ID) {
                adminCommand(chatId, text, message);
            } else {
                userCommand(chatId, text, userId);
            }
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            long chatId = message.getChatId();
            long userId = message.getFrom().getId();
            if (userId == ADMIN_CHAT_ID) {
                adminCommand(chatId, "Video received", message);
            } else {
                userCommand(chatId, message.getText(), userId);
            }
        } else if (update.hasCallbackQuery()) {
            returnAlertMessage(update);
        }
    }

    public void returnAlertMessage(Update update) throws TelegramApiException {
        CallbackQuery callbackQuery = update.getCallbackQuery();
        String callbackQueryId = callbackQuery.getId();
        AnswerCallbackQuery answerCallbackQuery = new AnswerCallbackQuery();
        answerCallbackQuery.setShowAlert(true);
        String data = callbackQuery.getData();
        Long chatId = callbackQuery.getFrom().getId();
        Integer messageId = callbackQuery.getMessage().getMessageId();
        answerCallbackQuery.setText("❌ Kechirasiz siz kanalga a'zo bo'lmadingiz");
        answerCallbackQuery.setCallbackQueryId(callbackQueryId);
        long userId = callbackQuery.getFrom().getId();
        if (data.equals("Tasdiqlash")) {
            boolean isSubscribed = checkSubscription(userId);
            if (isSubscribed) {
                execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
                sendUserMenuKeyboard(chatId);
            } else {
                try {
                    execute(answerCallbackQuery);  // Use the created AnswerCallbackQuery object
                } catch (TelegramApiException exception) {
                    exception.printStackTrace();
                }
            }
        }
    }

    public void userCommand(Long chatId, String text, Long userId) {
        boolean isSubscribed = checkSubscription(userId);
        if (text.equals("/start")) {
            if (isSubscribed) {
                sendUserMenuKeyboard(chatId);
            } else {
                sendInlineKeyboard(chatId);
            }
        } else if (isSubscribed) {
            if (text.equals("Kodli filmlar 🔐")) {
                sendTextMessage(chatId, "Film kodini yuboring:");
                info.put(chatId, "movie code");
            } else if (info.size() > 0) {
                getCodeMovie(chatId, text);
            }
        } else {
            sendInlineKeyboard(chatId);
        }
    }

    public void adminCommand(Long chatId, String text, Message message) throws TelegramApiException {
        if (text.equals("/start")) {
            sendAdminMenuKeyboard(chatId);
        } else if (text.equals("Kodli film qo'shish 🔐")) {
            sendTextMessage(chatId, "Film nomi kiriting:");
            info.put(chatId, "movie name");
        } else if (text.equals("Statistika 📊")) {
            int size = userSet.size();
            sendTextMessage(chatId, "👤 Foydalanuvchil soni: " + size + " nafar");
        } else if (text.equals("Filmlar ro'yxati 📋")) {
            List<Videos> allVideos = videosRepository.findAll();
            StringBuilder messageText = new StringBuilder("🎬 Filmlar ro'yxati 📋\n\n");
            if (!allVideos.isEmpty()) {
                for (Videos video : allVideos) {
                    int value = 0;
                    messageText.append(value + 1).append(". ").append(video.getName()).append(" ").append(video.getCode()).append("\n\n");
                }
                sendTextMessage(ADMIN_CHAT_ID, messageText.toString());
            } else {
                sendTextMessage(ADMIN_CHAT_ID, "Filmar ro'yxati bo'sh");
            }
        } else if (info.size() > 0) {
            addMovieCode(chatId, message);
        } else if (text.equals("Reklama joylash 🔊")) {
            sendTextMessage(chatId, "Reklamani menga yuboring");
        } else if (message.hasPhoto()) {
            sendTextMessage(chatId, "Reklama muoffaqqiyatli joylandi.");
            PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
            String fileId = photo.getFileId();
            InputFile photoInputFile = new InputFile(fileId);
            for (Long aLong : userSet) {
                execute(SendPhoto.builder().chatId(aLong).photo(photoInputFile).caption(message.getCaption()).build());
            }
        } else if (message.hasVideo()) {
            sendTextMessage(chatId, "Reklama muoffaqqiyatli joylandi.");
            Video video = message.getVideo();
            String fileId = video.getFileId();
            InputFile videoInputFile = new InputFile(fileId);
            for (Long aLong : userSet) {
                execute(SendVideo.builder().chatId(aLong).video(videoInputFile).caption(message.getCaption()).build());
            }
        } else if (message.hasText()) {
            sendTextMessage(chatId, text);
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

    private void sendUserMenuKeyboard(Long chatId) {
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        keyboardMarkup.setSelective(false);
        keyboardMarkup.setResizeKeyboard(true);
        keyboardMarkup.setOneTimeKeyboard(false);

        List<KeyboardRow> keyboard = new ArrayList<>();

        KeyboardRow keyboardButtonsRow1 = new KeyboardRow();
        keyboardButtonsRow1.add(new KeyboardButton("Kodli filmlar 🔐"));

        keyboard.add(keyboardButtonsRow1);

        keyboardMarkup.setKeyboard(keyboard);

        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText("Quyidagi bo'limlardan birini tanlang 👇");
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
        KeyboardRow keyboardButtonsRow3 = new KeyboardRow();

        keyboardButtonsRow1.add(new KeyboardButton("Kodli film qo'shish 🔐"));

        keyboardButtonsRow2.add(new KeyboardButton("Statistika 📊"));
        keyboardButtonsRow2.add(new KeyboardButton("Reklama joylash 🔊"));

        keyboardButtonsRow3.add(new KeyboardButton("Filmlar ro'yxati 📋"));

        keyboard.add(keyboardButtonsRow1);
        keyboard.add(keyboardButtonsRow2);
        keyboard.add(keyboardButtonsRow3);

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
                    sendTextMessage(chatId, "😔 Afsuski film topilmadi yoki kod xato bo'lishi mumkin.");
                }
            } else {
                sendTextMessage(chatId, "Siz hali telegram kanaga obuna bo'lmagansiz.");
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
                    Integer messageId = execute(SendMessage.builder().chatId(chatId).text("📥 Film saqlanmoqda...").build()).getMessageId();
                    infVid.put(chatId, message.getVideo());

                    Video video = infVid.get(chatId);
                    if (video != null) {
                        String fileId = video.getFileId();

                        byte[] videoBytes = downloadVideoBytes(fileId);

                        vid.put(chatId, video);
                        Videos build = Videos.builder()
                                .code(code.get(chatId))
                                .name(name.get(chatId))
                                .videoData(videoBytes)
                                .build();
                        videosRepository.save(build);
                        sendTextMessage(chatId, "Film saqlandi ✅");
                        execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                        infVid.remove(chatId);
                        infVid.clear();
                        info.clear();
                    } else {
                        sendTextMessage(chatId, "Video mavjud emas");
                    }
                } else {
                    sendTextMessage(chatId, "Iltimos video yuboring");
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
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

    public void sendTextMessage(Long chatId, String text) {
        try {
            execute(SendMessage.builder().chatId(chatId).text(text).build());
        } catch (TelegramApiException e) {
            System.err.println("Not Text");
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
// STOPSHIP: 21/01/2024
