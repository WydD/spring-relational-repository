package fr.petitl.relational.repository.query.parametered;

/**
 *
 */
public class StringQueryPart implements ParameteredQueryPart {
    private String fragment;

    public StringQueryPart(String fragment) {
        this.fragment = fragment;
    }

    @Override
    public String getFragment() {
        return fragment;
    }
}
