package com.voca.backend.toeic;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.ai.provider", havingValue = "local", matchIfMissing = true)
public class LocalToeicChatClient implements ToeicChatClient {

    @Override
    public String chat(String systemPrompt, String userPrompt) {
        return "Trợ lý AI chưa được bật (APP_AI_PROVIDER=local). "
                + "Bạn có thể xem lại đáp án đúng và phần giải thích có sẵn ở trên. "
                + "Để dùng chatbot giải thích chi tiết, hãy cấu hình APP_AI_PROVIDER=openai hoặc gemini.";
    }
}
