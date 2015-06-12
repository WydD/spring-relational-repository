package fr.petitl.relational.pg;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.petitl.relational.pg.json.JsonField;
import fr.petitl.relational.pg.json.JsonProxy;
import fr.petitl.relational.repository.dialect.generic.SpringJDBCAttributeMapper;
import org.postgis.Geometry;
import org.postgis.PGgeometry;
import org.postgresql.util.PGobject;

import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * Created by loic on 11/06/15.
 */
public class PGAttributeMapper extends SpringJDBCAttributeMapper {

    // Jackson Mapper
    private static final ObjectMapper om = new ObjectMapper();

    @Override
    public void writeAttribute(PreparedStatement ps, int column, Object o, Field sourceField) throws SQLException {
        if (sourceField != null && sourceField.getAnnotation(JsonField.class) != null) {
            writeJson(ps, column, o, sourceField.getAnnotation(JsonField.class).binary());
            return;
        }
        if (o instanceof JsonProxy) {
            final JsonProxy proxy = (JsonProxy) o;
            writeJson(ps, column, proxy.getObject(), proxy.isBinary());
            return;
        }
        if (o instanceof Geometry) {
            // Convert into geometry
            o = new PGgeometry((Geometry) o);
        }
        if (o instanceof PGobject) {
            ps.setObject(column, o);
            return;
        }
        super.writeAttribute(ps, column, o, sourceField);
    }

    private void writeJson(PreparedStatement ps, int column, Object o, boolean binary) throws SQLException {
        // You have to put the correct object in the prepared statement
        // In this case, convert the object into a JSON string that you encapsulate in a PGobject
        PGobject pg = new PGobject();
        pg.setType(binary ? "jsonb" : "json");
        try {
            // Pure JSON serialization
            pg.setValue(om.writeValueAsString(o));
            ps.setObject(column, pg);
        } catch (JsonProcessingException e) {
            // standard exception handling
            throw new SQLException(e);
        }
    }

    @Override
    public Object readAttribute(ResultSet rs, int column, Field targetField) throws SQLException {
        // Manage the json type
        if (targetField.getAnnotation(JsonField.class) != null) {
            if (rs.getString(column) == null)
                return null;
            // You have to extract the object in the right type from the JDBC result set
            // In this case, convert the JSON string to the desired type
            JavaType targetType = om.getTypeFactory().constructType(targetField.getGenericType());
            try {
                return om.readValue(rs.getString(column), targetType);
            } catch (IOException e) {
                // standard exception handling
                throw new SQLException(e);
            }
        }
        // Manage all extended object from the driver (postgis, money, interval...)
        if (Geometry.class.isAssignableFrom(targetField.getType())) {
            final PGgeometry pg = (PGgeometry) rs.getObject(column);
            return pg.getGeometry();
        }
        // Manage all extended object from the driver (postgis, money, interval...)
        if (PGobject.class.isAssignableFrom(targetField.getType())) {
            return rs.getObject(column);
        }
        // Standard is managed by the standard
        return super.readAttribute(rs, column, targetField);
    }
}
