package com.exchange.exchange.converter;

import com.exchange.exchange.enums.TradeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// TradeTypeConverter: 自動將 TradeType 轉換為小寫字串存入資料庫。
@Converter(autoApply = true)
public class TradeTypeConverter implements AttributeConverter<TradeType, String> {

    @Override
    public String convertToDatabaseColumn(TradeType attribute) {
        if (attribute == null) return null;
        // SPOT -> "spot"
        return attribute.name().toLowerCase();
    }

    @Override
    public TradeType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        // "spot" -> SPOT
        return TradeType.valueOf(dbData.toUpperCase());
    }
}