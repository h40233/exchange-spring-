package com.exchange.exchange.converter;

import com.exchange.exchange.enums.PositionStatus;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// ====== 檔案總結 ======
// PositionStatusConverter: OPEN -> "open"
@Converter(autoApply = true)
public class PositionStatusConverter implements AttributeConverter<PositionStatus, String> {

    @Override
    public String convertToDatabaseColumn(PositionStatus attribute) {
        if (attribute == null) return null;
        return attribute.name().toLowerCase();
    }

    @Override
    public PositionStatus convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        return PositionStatus.valueOf(dbData.toUpperCase());
    }
}