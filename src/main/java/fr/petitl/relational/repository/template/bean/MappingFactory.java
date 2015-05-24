package fr.petitl.relational.repository.template.bean;

import java.util.HashMap;
import java.util.Map;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.template.RelationalTemplate;

/**
 *
 */
public class MappingFactory {

    private Map<Class, BeanMappingData> cache = new HashMap<>();
    private BeanDialect dialect;

    public MappingFactory(BeanDialect dialect) {
        this.dialect = dialect;
    }

    @SuppressWarnings("unchecked")
    public synchronized <T> BeanMappingData<T> beanMapping(Class<T> clazz, RelationalTemplate template) {
        BeanMappingData data = cache.get(clazz);
        if (data == null) {
            BeanMappingData<T> result = new BeanMappingData<T>(clazz, dialect, template);
            cache.put(clazz, result);
            return result;
        }
        return data;
    }


}
