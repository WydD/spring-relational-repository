package fr.petitl.relational.repository.template;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcUtils;

/**
 *
 */
public class RelationalTemplateBak extends JdbcTemplate {

    public RelationalTemplateBak() {
    }

    public RelationalTemplateBak(DataSource dataSource) {
        super(dataSource);
    }

    public RelationalTemplateBak(DataSource dataSource, boolean lazyInit) {
        super(dataSource, lazyInit);
    }

    public Stream<ResultSet> stream(String sql) {
        Connection con = DataSourceUtils.getConnection(getDataSource());
        Statement stmt = null;
        try {
            Connection conToUse = con;
            if (this.getNativeJdbcExtractor() != null &&
                    this.getNativeJdbcExtractor().isNativeConnectionNecessaryForNativeStatements()) {
                conToUse = this.getNativeJdbcExtractor().getNativeConnection(con);
            }
            stmt = conToUse.createStatement();
            applyStatementSettings(stmt);
            if (this.getNativeJdbcExtractor() != null) {
                stmt = this.getNativeJdbcExtractor().getNativeStatement(stmt);
            }

            final ResultSet rs =  stmt.executeQuery(sql);
            final Statement finalStmt = stmt;
            final Connection finalCon = con;
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ResultSetIteratorBak(rs), 0), false).onClose(() -> {
                JdbcUtils.closeResultSet(rs);
                JdbcUtils.closeStatement(finalStmt);
                DataSourceUtils.releaseConnection(finalCon, getDataSource());
            });
        }
        catch (SQLException ex) {
            // Release Connection early, to avoid potential connection pool deadlock
            // in the case when the exception translator hasn't been initialized yet.
            JdbcUtils.closeStatement(stmt);
            stmt = null;
            DataSourceUtils.releaseConnection(con, getDataSource());
            con = null;
            throw getExceptionTranslator().translate("StatementCallback", sql, ex);
        }
    }
}
