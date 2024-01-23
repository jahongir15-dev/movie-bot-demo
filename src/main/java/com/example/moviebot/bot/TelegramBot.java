package com.example.moviebot.bot;

import com.example.moviebot.entity.Videos;
import com.example.moviebot.repository.VideosRepository;
import lombok.RequiredArgsConstructor;
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

@RequiredArgsConstructor
public class TelegramBot extends TelegramLongPollingBot {

    private final VideosRepository videosRepository;

    @Override
    public String getBotUsername() {
        return BotConfig.BOT_USERNAME;
    }

    @Override
    public String getBotToken() {
        return BotConfig.BOT_TOKEN;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            Long userId = message.getFrom().getId();
            BotConfig.IS_USERS.add(userId);
            BotConfig.IS_USERS.remove(BotConfig.ADMIN_CHAT_ID);
            String text = message.getText();
            if (userId.equals(BotConfig.ADMIN_CHAT_ID)) {
                adminCommand(chatId, text, message);
            } else {
                userCommand(chatId, text, userId);
            }
        } else if (update.hasMessage()) {
            Message message = update.getMessage();
            Long chatId = message.getChatId();
            Long userId = message.getFrom().getId();
            if (userId.equals(BotConfig.ADMIN_CHAT_ID)) {
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
        Long userId = callbackQuery.getFrom().getId();
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
                BotConfig.IS_MOVIE_INF.put(chatId, "movie code");
            } else if (BotConfig.IS_MOVIE_INF.size() > 0) {
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
            BotConfig.IS_MOVIE_INF.put(chatId, "movie name");
        } else if (text.equals("Statistika 📊")) {
            int size = BotConfig.IS_USERS.size();
            sendTextMessage(chatId, "👤 Foydalanuvchil soni: " + size + " nafar");
        } else if (text.equals("Filmlar ro'yxati 📋")) {
            List<Videos> allVideos = videosRepository.findAll();
            StringBuilder messageText = new StringBuilder("🎬 Filmlar ro'yxati 📋\n\n");
            if (!allVideos.isEmpty()) {
                for (Videos video : allVideos) {
                    int value = 0;
                    messageText.append(value + 1).append(". ").append(video.getName()).append(" ").append(video.getCode()).append("\n\n");
                }
                sendTextMessage(BotConfig.ADMIN_CHAT_ID, messageText.toString());
            } else {
                sendTextMessage(BotConfig.ADMIN_CHAT_ID, "Filmar ro'yxati bo'sh");
            }
        } else if (BotConfig.IS_MOVIE_INF.size() > 0) {
            addMovie(chatId, message);
        } else if (text.equals("Reklama joylash 🔊")) {
            sendTextMessage(chatId, "Reklamani menga yuboring");
            BotConfig.IS_ADD.put(chatId, "add");

        } else if (BotConfig.IS_ADD.size() > 0) {
            if (message.hasPhoto()) {
                sendTextMessage(chatId, "Reklama muoffaqqiyatli joylandi.");
                PhotoSize photo = message.getPhoto().get(message.getPhoto().size() - 1);
                String fileId = photo.getFileId();
                InputFile photoInputFile = new InputFile(fileId);
                for (Long aLong : BotConfig.IS_USERS) {
                    execute(SendPhoto.builder()
                            .chatId(aLong).photo(photoInputFile).caption(message.getCaption()).build());
                }
            } else if (message.hasVideo()) {
                sendTextMessage(chatId, "Reklama muoffaqqiyatli joylandi.");
                Video video = message.getVideo();
                String fileId = video.getFileId();
                InputFile videoInputFile = new InputFile(fileId);
                for (Long aLong : BotConfig.IS_USERS) {
                    execute(SendVideo.builder().chatId(aLong).video(videoInputFile).caption(message.getCaption()).build());
                }
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
        if (BotConfig.IS_MOVIE_INF.get(chatId).equals("movie code")) {
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
                                .caption("🎬Kino: " + videosByCode.getName() + "\n\uD83C\uDDFA\uD83C\uDDFFTarjima: " + videosByCode.getLanguage() + "\n" +
                                        "🌍Davlat: " + videosByCode.getCountry() + "\n🎞️Sifati: " + videosByCode.getQuality() + "\n⚔Janri: " + videosByCode.getGenre())
                                .build());
                    } catch (TelegramApiException exception) {
                        exception.printStackTrace();
                    }

                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_MOVIE_INF.clear();
                } else {
                    sendTextMessage(chatId, "😔 Afsuski film topilmadi yoki kod xato bo'lishi mumkin.");
                }
            } else {
                sendTextMessage(chatId, "Siz hali telegram kanaga obuna bo'lmagansiz.");
            }
        }
    }

    private void addMovie(Long chatId, Message message) {
        try {
            switch (BotConfig.IS_MOVIE_INF.get(chatId)) {
                case "movie name":
                    sendTextMessage(chatId, "Film davlatini kiriting:");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_NAME.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "country");
                    break;
                case "country":
                    sendTextMessage(chatId, "Film qaysi tilda ?");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_COUNTRY.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "language");
                    break;
                case "language":
                    sendTextMessage(chatId, "Film sifatini kiriting:");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_LANGUAGE.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "quality");
                    break;
                case "quality":
                    sendTextMessage(chatId, "Film janrini kiriting:");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_QUALITY.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "genre");
                    break;
                case "genre":
                    sendTextMessage(chatId, "Film kodini kiriting:");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_GENRE.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "code");
                    break;
                case "code":
                    sendTextMessage(chatId, "Film yuboring:");
                    BotConfig.IS_MOVIE_INF.remove(chatId);
                    BotConfig.IS_CODE.put(chatId, message.getText());
                    BotConfig.IS_MOVIE_INF.put(chatId, "video");
                    break;
                case "video":
                    if (message.getVideo() != null) {
                        Integer messageId = execute(SendMessage.builder().chatId(chatId).text("📥 Film saqlanmoqda...").build()).getMessageId();
                        BotConfig.IS_VIDEO_INFO.put(chatId, message.getVideo());

                        Video video = BotConfig.IS_VIDEO_INFO.get(chatId);
                        if (video != null) {
                            String fileId = video.getFileId();

                            byte[] videoBytes = downloadVideoBytes(fileId);

                            System.out.println(video.getFileUniqueId());

                            BotConfig.IS_VIDEO.put(chatId, video);
                            Videos build = Videos.builder()
                                    .name(BotConfig.IS_NAME.get(chatId))
                                    .country(BotConfig.IS_COUNTRY.get(chatId))
                                    .language(BotConfig.IS_LANGUAGE.get(chatId))
                                    .quality(BotConfig.IS_QUALITY.get(chatId))
                                    .genre(BotConfig.IS_GENRE.get(chatId))
                                    .code(BotConfig.IS_CODE.get(chatId))
                                    .videoData(videoBytes)
                                    .fileUniqueId(video.getFileUniqueId())
                                    .build();
                            videosRepository.save(build);
                            sendTextMessage(chatId, "Film saqlandi ✅");
                            execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());

                            BotConfig.IS_VIDEO_INFO.remove(chatId);
                            BotConfig.IS_VIDEO_INFO.clear();
                            BotConfig.IS_MOVIE_INF.clear();
                        } else {
                            sendTextMessage(chatId, "Video mavjud emas");
                        }
                    } else {
                        sendTextMessage(chatId, "Iltimos video yuboring");
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private byte[] downloadVideoBytes(String fileId) throws TelegramApiException {
        try {
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
        } catch (TelegramApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TelegramApiException("Error video", e);
        }
    }

    private boolean checkSubscription(Long userId) {
        try {
            GetChatMember getChatMember = new GetChatMember(BotConfig.CHANNEL_USERNAME, userId);
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
}
