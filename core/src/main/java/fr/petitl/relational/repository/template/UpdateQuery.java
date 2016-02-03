package fr.petitl.relational.repository.template;

/**
 *
 */
public class UpdateQuery extends AbstractQuery<UpdateQuery> {

    public UpdateQuery(String sql, RelationalTemplate template) {
        super(sql, template);
    }

    public int execute() {
        return template.executeUpdate(query.getQueryString(), null, getPrepareStatement());
    }

}
