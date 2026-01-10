package com.exchange.exchange.converter;

// 引入訂單類型枚舉
import com.exchange.exchange.enums.OrderType;
// 引入 JPA 轉換器介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// OrderTypeConverter 負責將 OrderType 枚舉轉換為小寫字串存入資料庫。
// 映射規則：
// MARKET -> "market"
// LIMIT -> "limit"
@Converter(autoApply = true)
public class OrderTypeConverter implements AttributeConverter<OrderType, String> {

    // 將枚舉轉換為資料庫字串 (小寫)
    @Override
    public String convertToDatabaseColumn(OrderType attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    // 將資料庫字串轉換回枚舉 (轉大寫後比對)
    @Override
    public OrderType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return OrderType.valueOf(dbData.toUpperCase());
    }
}