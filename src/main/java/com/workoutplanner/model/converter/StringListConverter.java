package com.workoutplanner.model.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JPA attribute converter: List<String> ↔ comma-delimited TEXT.
 * <p>
 * PostgreSQL stores these columns as TEXT[] via the Flyway migration;
 * H2 (test profile, Hibernate DDL) creates them as TEXT and this
 * converter serialises the list as a comma-delimited string so that
 * both databases work transparently.
 */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String DELIMITER = ",";

    @Override
    public String convertToDatabaseColumn(List<String> attribute) {
        if (attribute == null || attribute.isEmpty()) {
            return null;
        }
        return String.join(DELIMITER, attribute);
    }

    @Override
    public List<String> convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.asList(dbData.split(DELIMITER, -1));
    }
}
