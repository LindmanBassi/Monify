package br.com.bassi.monify.domain.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.LocalDate;
import java.time.YearMonth;

@Converter(autoApply = true)
public class YearMonthConverter implements AttributeConverter<YearMonth, LocalDate> {

    @Override
    public LocalDate convertToDatabaseColumn(YearMonth mes) {
        return mes == null ? null : mes.atDay(1);
    }

    @Override
    public YearMonth convertToEntityAttribute(LocalDate data) {
        return data == null ? null : YearMonth.from(data);
    }
}
