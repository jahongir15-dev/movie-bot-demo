package com.example.moviebot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.send.SendVideo;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMember;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@SpringBootApplication
public class MovieBotApplication extends TelegramLongPollingBot {

    public static void main(String[] args) {
        try {
            SpringApplication.run(MovieBotApplication.class, args);
            TelegramBotsApi telegramBotsApi = new TelegramBotsApi(DefaultBotSession.class);
            telegramBotsApi.registerBot(new MovieBotApplication());
        } catch (TelegramApiException error) {
            System.err.println("Not Api");
        }
    }

    @Override
    public String getBotUsername() {
        return "movie_demo_uz_bot";
    }

    @Override
    public String getBotToken() {
        return "6923550332:AAH2yu-lqiEyqqML_3tKfl7PtX2g3YnASp0";
    }

    private static final String CHANNEL_USERNAME = "@test_channel_demo";

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            Message message = update.getMessage();
            long chatId = update.getMessage().getChatId();
            long userId = update.getMessage().getFrom().getId();
            String text = message.getText();

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
                sendTextMessage(chatId,"Film kodini kiriting");
            }
        } else if (update.hasCallbackQuery()) {
            CallbackQuery callbackQuery = update.getCallbackQuery();
            String data = callbackQuery.getData();
            Long chatId = callbackQuery.getFrom().getId();
            long userId = update.getCallbackQuery().getFrom().getId();
            if (data.equals("Tasdiqlash")) {
                boolean isSubscribed = checkSubscription(userId);
                if (isSubscribed) {
                    deleteMessage(chatId, callbackQuery.getMessage().getMessageId());
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

    private void deleteMessage(Long chatId, Integer messageId) {
        try {
            execute(DeleteMessage.builder().chatId(chatId).messageId(messageId).build());
        } catch (TelegramApiException e) {
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

}
