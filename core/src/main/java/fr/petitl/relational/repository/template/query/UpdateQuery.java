package fr.petitl.relational.repository.template.query;

import fr.petitl.relational.repository.template.RelationalTemplate;

/**
 *
 */
public class UpdateQuery extends AbstractQuery<UpdateQuery> {

    public UpdateQuery(String sql, RelationalTemplate template) {
        super(sql, template);
    }

    public int execute() {
        return template.executeUpdate(query.getQueryString(), query);
    }

}
