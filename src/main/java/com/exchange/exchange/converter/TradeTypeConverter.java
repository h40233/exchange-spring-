package com.exchange.exchange.converter;

// 引入交易類型枚舉
import com.exchange.exchange.enums.TradeType;
// 引入 JPA 轉換器介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// TradeTypeConverter 負責將 TradeType 枚舉轉換為小寫字串存入資料庫。
// 映射規則：
// SPOT -> "spot"
// CONTRACT -> "contract"
@Converter(autoApply = true)
public class TradeTypeConverter implements AttributeConverter<TradeType, String> {

    // 將枚舉轉換為資料庫字串 (小寫)
    @Override
    public String convertToDatabaseColumn(TradeType attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    // 將資料庫字串轉換回枚舉 (轉大寫後比對)
    @Override
    public TradeType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return TradeType.valueOf(dbData.toUpperCase());
    }
}