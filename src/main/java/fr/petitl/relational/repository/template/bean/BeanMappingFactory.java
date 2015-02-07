package fr.petitl.relational.repository.template.bean;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class BeanMappingFactory {

    private static Map<Class, BeanMappingData> cache = new HashMap<>();

    @SuppressWarnings("unchecked")
    public synchronized static <T> BeanMappingData<T> get(Class<T> clazz) {
        BeanMappingData data = cache.get(clazz);
        if (data == null) {
            BeanMappingData<T> result = new BeanMappingData<T>(clazz);
            cache.put(clazz, result);
            return result;
        }
        return data;
    }

}
