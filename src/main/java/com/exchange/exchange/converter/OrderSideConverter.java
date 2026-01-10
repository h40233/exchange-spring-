package com.exchange.exchange.converter;

// 引入訂單方向枚舉
import com.exchange.exchange.enums.OrderSide;
// 引入 JPA 屬性轉換器介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// OrderSideConverter 負責處理 Java Enum (OrderSide) 與資料庫欄位 (String) 之間的轉換邏輯。
// 使用 @Converter(autoApply = true) 註解，表示 JPA 在遇到任何 OrderSide 類型的實體屬性時，
// 都會自動使用此轉換器，無需在每個 Entity 欄位上額外標註 @Convert。
@Converter(autoApply = true)
public class OrderSideConverter implements AttributeConverter<OrderSide, String> {

    // 方法：將 Java 物件轉換為資料庫儲存格式
    // 觸發時機：當 Entity 被保存 (persist) 或更新 (update) 至資料庫時
    @Override
    public String convertToDatabaseColumn(OrderSide attribute) {
        // 防禦性檢查：若屬性為 null，則回傳 null (資料庫欄位允許 NULL 時)
        if (attribute == null) {
            return null;
        }
        // 將枚舉名稱轉為小寫字串 (例如 OrderSide.BUY -> "buy")
        // 這是為了配合 MySQL 中 ENUM('buy', 'sell') 的小寫定義 [註1]
        return attribute.name().toLowerCase();
    }

    // 方法：將資料庫欄位值轉換為 Java 物件
    // 觸發時機：從資料庫讀取 (select) 資料並映射回 Entity 時
    @Override
    public OrderSide convertToEntityAttribute(String dbData) {
        // 防禦性檢查：若資料庫值為 null，則回傳 null
        if (dbData == null) {
            return null;
        }
        // 將資料庫的小寫字串轉為大寫，再查找對應的 Enum (例如 "buy" -> "BUY" -> OrderSide.BUY)
        // 若 dbData 不在 Enum 定義中，這裡會拋出 IllegalArgumentException
        return OrderSide.valueOf(dbData.toUpperCase());
    }
}
// ====== 備註區 ======
/*
[註1] 大小寫敏感性 (Case Sensitivity):
      此轉換器假設資料庫儲存的是小寫 ("buy")，而 Java Enum 是大寫 (BUY)。
      這是常見的映射模式，但必須確保資料庫 Schema 定義確實也是小寫。
      若資料庫中混用了 "Buy" 或 "BUY"，此轉換邏輯可能會失敗。
*/