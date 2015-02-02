package fr.petitl.relational.repository.template;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Iterator;

import org.springframework.dao.DataAccessException;
import org.springframework.jca.cci.InvalidResultSetAccessException;

public class ResultSetIteratorBak implements Iterator<ResultSet> {

    private ResultSet rs;

    public ResultSetIteratorBak(ResultSet rs) {
        this.rs = rs;
    }


    @Override
    public boolean hasNext() {
        try {
            return rs.next();
        } catch (SQLException e) {
            throw new InvalidResultSetAccessException("While doing rs.next()", e);
        }

    }

    @Override
    public ResultSet next() {
        return rs;
    }
}