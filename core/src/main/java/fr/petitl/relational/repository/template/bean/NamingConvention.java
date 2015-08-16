package fr.petitl.relational.repository.template.bean;

import java.lang.reflect.Field;

/**
 *
 */
public interface NamingConvention {
    String generateDefaultColumnName(Field field);
}
