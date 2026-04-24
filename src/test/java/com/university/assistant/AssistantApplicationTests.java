package com.university.assistant;

import org.junit.jupiter.api.Test;

// Наследуемся от BaseIntegrationTest, чтобы тест знал, 
// где взять базу данных (Docker) и фейковые ключи API
class AssistantApplicationTests extends BaseIntegrationTest {

    @Test
    void contextLoads() {
        // Этот тест просто проверяет, что Spring Context запускается успешно
        // со всеми нашими бинами и настройками.
    }

}