package com.exchange.exchange;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// ====== 檔案總結 ======
// ExchangeApplicationTests 是一個冒煙測試 (Smoke Test)。
// 它僅嘗試啟動 Spring Application Context。
// 如果設定檔錯誤、Bean 依賴注入失敗或資料庫連線異常，此測試將會失敗。
// 這通常是 CI/CD 流程中的第一道關卡。
@SpringBootTest
class ExchangeApplicationTests {

    @Test
    void contextLoads() {
        // 不需要撰寫任何邏輯
        // 只要能執行到這裡，代表 Spring 容器啟動成功
    }

}