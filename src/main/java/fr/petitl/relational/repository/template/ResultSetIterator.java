package fr.petitl.relational.repository.template;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import fr.petitl.relational.repository.repository.StreamSqlException;

public class ResultSetIterator implements Iterator<ResultSet> {

    private ResultSet rs;

    public ResultSetIterator(ResultSet rs) {
        this.rs = rs;
    }


    @Override
    public boolean hasNext() {
        try {
            return rs.next();
        } catch (SQLException e) {
            throw new StreamSqlException(e);
        }

    }

    @Override
    public ResultSet next() {
        return rs;
    }
}