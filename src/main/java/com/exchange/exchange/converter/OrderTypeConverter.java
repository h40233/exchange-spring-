package com.exchange.exchange.converter;

import com.exchange.exchange.enums.OrderType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// OrderTypeConverter: LIMIT -> "limit"
@Converter(autoApply = true)
public class OrderTypeConverter implements AttributeConverter<OrderType, String> {

    @Override
    public String convertToDatabaseColumn(OrderType attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public OrderType convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return OrderType.valueOf(dbData.toUpperCase());
    }
}