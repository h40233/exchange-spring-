package com.exchange.exchange.converter;

import com.exchange.exchange.enums.PositionSide;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class PositionSideConverter implements AttributeConverter<PositionSide, String> {

    @Override
    public String convertToDatabaseColumn(PositionSide attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public PositionSide convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return PositionSide.valueOf(dbData.toUpperCase());
    }
}
