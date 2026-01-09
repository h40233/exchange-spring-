package com.exchange.exchange.converter;

import com.exchange.exchange.enums.Timeframe;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.stream.Stream;

@Converter(autoApply = true)
public class TimeframeConverter implements AttributeConverter<Timeframe, String> {

    @Override
    public String convertToDatabaseColumn(Timeframe attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    @Override
    public Timeframe convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return Stream.of(Timeframe.values())
                .filter(c -> c.getValue().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown timeframe: " + dbData));
    }
}
