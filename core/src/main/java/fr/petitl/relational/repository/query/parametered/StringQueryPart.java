package fr.petitl.relational.repository.query.parametered;

/**
 *
 */
public class StringQueryPart implements ParameteredQueryPart {
    private String fragment;

    public StringQueryPart(String fragment) {
        this.fragment = fragment;
    }

    public String trimFragment() {
        fragment = fragment.trim();
        return fragment;
    }

    @Override
    public String getFragment() {
        return fragment;
    }
}
