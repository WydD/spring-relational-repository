package fr.petitl.relational.repository.simple.repository.impl;

import fr.petitl.relational.repository.simple.PojoDTO;
import fr.petitl.relational.repository.simple.repository.PojoRepositoryCustom;
import fr.petitl.relational.repository.template.RelationalTemplate;
import org.springframework.beans.factory.annotation.Autowired;

/**
 *
 */
public class PojoRepositoryImpl implements PojoRepositoryCustom {

    @Autowired
    private RelationalTemplate template;

    @Override
    public int dummy() {
        return 42;
    }

    public PojoDTO testCustomMapper() {
        String sql = "SELECT id, name FROM Pojo WHERE name = 'ho'";
        return template.createQuery(sql, rs -> {
            PojoDTO pojoDTO = new PojoDTO();
            pojoDTO.setId(rs.getString("id"));
            pojoDTO.setName(rs.getString("name"));
            return pojoDTO;
        }).findOne();
    }
}
