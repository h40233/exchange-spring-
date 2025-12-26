package com.exchange.exchange.converter;

import com.exchange.exchange.enums.OrderSide;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter(autoApply = true)
public class OrderSideConverter implements AttributeConverter<OrderSide, String> {

    @Override
    public String convertToDatabaseColumn(OrderSide attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.name().toLowerCase();
    }

    @Override
    public OrderSide convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        return OrderSide.valueOf(dbData.toUpperCase());
    }
}
