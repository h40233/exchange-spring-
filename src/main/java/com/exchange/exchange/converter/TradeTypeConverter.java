package com.exchange.exchange.converter;

import com.exchange.exchange.enums.TradeType;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class TradeTypeConverter implements AttributeConverter<TradeType, String> {

    @Override
    public String convertToDatabaseColumn(TradeType attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public TradeType convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return TradeType.valueOf(dbData.toUpperCase());
    }
}
