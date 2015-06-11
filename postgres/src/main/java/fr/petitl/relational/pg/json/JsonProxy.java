package fr.petitl.relational.pg.json;

/**
 * Forces the conversion between an object and "json(b)" field in postgres
 */
public class JsonProxy {

    private final Object o;
    private boolean binary;

    /**
     * Create a proxy
     * @param o The object
     * @param binary Whether this object must be stored in jsonb (binary) or not
     */
    public JsonProxy(Object o, boolean binary) {
        this.o = o;
        this.binary = binary;
    }

    public Object getObject() {
        return o;
    }

    public boolean isBinary() {
        return binary;
    }
}
