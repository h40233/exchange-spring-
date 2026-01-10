package com.exchange.exchange.converter;

// 引入訂單狀態枚舉
import com.exchange.exchange.enums.OrderStatus;
// 引入 JPA 轉換器介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// OrderStatusConverter 負責將 OrderStatus 枚舉轉換為小寫字串存入資料庫，反之亦然。
// 映射規則：
// NEW -> "new"
// PARTIAL_FILLED -> "partial_filled"
// FILLED -> "filled"
// CANCELED -> "canceled"
@Converter(autoApply = true)
public class OrderStatusConverter implements AttributeConverter<OrderStatus, String> {

    // 將枚舉轉換為資料庫字串 (小寫)
    @Override
    public String convertToDatabaseColumn(OrderStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    // 將資料庫字串轉換回枚舉 (轉大寫後比對)
    @Override
    public OrderStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return OrderStatus.valueOf(dbData.toUpperCase());
    }
}