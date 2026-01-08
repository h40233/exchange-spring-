package com.exchange.exchange.converter;

// 引入枚舉
import com.exchange.exchange.enums.OrderSide;
// 引入 JPA Converter 介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// OrderSideConverter 負責 Java Enum 與 資料庫欄位 之間的轉換。
// @Converter(autoApply = true): 表示此轉換器會自動應用到所有使用 `OrderSide` 類型的實體欄位上，
// 無需在每個 Entity 欄位上額外標註 @Convert。
@Converter(autoApply = true)
public class OrderSideConverter implements AttributeConverter<OrderSide, String> {

    // 將 Java 物件 (Enum) 轉換為 資料庫資料 (String)
    // 寫入資料庫時呼叫
    @Override
    public String convertToDatabaseColumn(OrderSide attribute) {
        if (attribute == null) {
            return null;
        }
        // 將枚舉轉為小寫存入資料庫 (例如 BUY -> "buy")
        // 這通常是為了配合 MySQL ENUM 類型的定義 (enum('buy','sell'))
        return attribute.name().toLowerCase();
    }

    // 將 資料庫資料 (String) 轉換為 Java 物件 (Enum)
    // 從資料庫讀取時呼叫
    @Override
    public OrderSide convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 將資料庫的小寫字串轉為大寫，再查找對應的 Enum (例如 "buy" -> BUY)
        return OrderSide.valueOf(dbData.toUpperCase());
    }
}
// ====== 備註區 ======
/*
[註1] 大小寫敏感性 (Case Sensitivity):
      此轉換器假設資料庫儲存的是小寫 ("buy")，而 Java Enum 是大寫 (BUY)。
      這是一種常見的映射模式，但必須確保資料庫 Schema 定義確實也是小寫，否則會發生錯誤。
*/