package com.exchange.exchange.converter;

// 引入時間週期枚舉
import com.exchange.exchange.enums.Timeframe;
// 引入 JPA 轉換器介面
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

// 引入 Stream API
import java.util.stream.Stream;

// ====== 檔案總結 ======
// TimeframeConverter 負責 Timeframe 枚舉與資料庫字串的轉換。
// 由於 Timeframe 枚舉名稱 (如 _1m) 與實際值 (如 "1m") 可能不同 (Java 變數不能以數字開頭)，
// 因此這裡不能單純用 name()，而是使用 getValue()。
@Converter(autoApply = true)
public class TimeframeConverter implements AttributeConverter<Timeframe, String> {

    // 將枚舉轉換為資料庫字串
    // 這裡呼叫 attribute.getValue() 來獲取自定義的字串值 (例如 "1m", "1H")
    @Override
    public String convertToDatabaseColumn(Timeframe attribute) {
        if (attribute == null) {
            return null;
        }
        return attribute.getValue();
    }

    // 將資料庫字串轉換回枚舉
    @Override
    public Timeframe convertToEntityAttribute(String dbData) {
        if (dbData == null) {
            return null;
        }
        // 遍歷所有枚舉值，尋找 value 與 dbData 相符的項目
        // 若找不到則拋出異常，提示資料庫中有未知的值
        return Stream.of(Timeframe.values())
                .filter(c -> c.getValue().equals(dbData))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown timeframe: " + dbData));
    }
}