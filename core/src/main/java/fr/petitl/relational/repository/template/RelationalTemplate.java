package fr.petitl.relational.repository.template;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Iterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import fr.petitl.relational.repository.dialect.BeanDialect;
import fr.petitl.relational.repository.template.bean.BeanMappingData;
import fr.petitl.relational.repository.template.bean.CamelToSnakeConvention;
import fr.petitl.relational.repository.template.bean.MappingFactory;
import fr.petitl.relational.repository.template.bean.NamingConvention;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.JdbcAccessor;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.nativejdbc.NativeJdbcExtractor;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.Assert;

/**
 *
 */
public class RelationalTemplate extends JdbcAccessor implements ApplicationContextAware {
    private static final Logger log = LoggerFactory.getLogger(RelationalTemplate.class);

    protected final BeanDialect dialect;
    protected final MappingFactory mappingFactory;
    protected NativeJdbcExtractor nativeJdbcExtractor;
    protected TransactionTemplate transactionTemplate;
    protected final NamingConvention namingConvention;

    public RelationalTemplate(DataSource ds, BeanDialect dialect) {
        this(ds, dialect, new CamelToSnakeConvention());
    }

    public RelationalTemplate(DataSource ds, BeanDialect dialect, NamingConvention namingConvention) {
        this.dialect = dialect;
        mappingFactory = new MappingFactory(dialect);
        this.setDataSource(ds);
        afterPropertiesSet();
        this.namingConvention = namingConvention;
    }

    public BeanDialect getDialect() {
        return dialect;
    }

    public <T> BeanMappingData<T> getMappingData(Class<T> clazz) {
        return mappingFactory.beanMapping(clazz, this);
    }


    public NativeJdbcExtractor getNativeJdbcExtractor() {
        return nativeJdbcExtractor;
    }

    public void setNativeJdbcExtractor(NativeJdbcExtractor nativeJdbcExtractor) {
        this.nativeJdbcExtractor = nativeJdbcExtractor;
    }

    public <T> T execute(ConnectionCallback<T> action) throws DataAccessException {
        return executeDontClose((Connection it) -> {
            try {
                return action.doInConnection(it);
            } finally {
                release(it);
            }
        });
    }

    protected <T> T executeDontClose(ConnectionCallback<T> action) throws DataAccessException {
        Assert.notNull(action, "Callback object must not be null");
        Connection con = DataSourceUtils.getConnection(getDataSource());
        return translateExceptions("ConnectionCallback", null, () -> {
            try {
                Connection conToUse = con;
                if (this.getNativeJdbcExtractor() != null &&
                        this.getNativeJdbcExtractor().isNativeConnectionNecessaryForNativeStatements()) {
                    conToUse = this.getNativeJdbcExtractor().getNativeConnection(con);
                }
                return action.doInConnection(conToUse);
            } catch (Exception e) {
                release(con);
                throw e;
            }
        });
    }


    public <E extends Statement, T> T execute(StatementCallback<E, T> action, StatementProvider<E> supplier)
            throws DataAccessException {
        return executeDontClose((E st) -> {
            try {
                return action.execute(st);
            } finally {
                release(st);
            }
        }, supplier);
    }

    protected <E extends Statement, T> T executeDontClose(StatementCallback<E, T> action, StatementProvider<E> supplier)
            throws DataAccessException {
        return executeDontClose((Connection con) -> {
            E stmt = null;
            try {
                stmt = supplier.statement(con);
                if (nativeJdbcExtractor != null) {
                    if (stmt instanceof CallableStatement) {
                        //noinspection unchecked
                        stmt = (E) nativeJdbcExtractor.getNativeCallableStatement((CallableStatement) stmt);
                    } else if (stmt instanceof PreparedStatement) {
                        //noinspection unchecked
                        stmt = (E) nativeJdbcExtractor.getNativePreparedStatement((PreparedStatement) stmt);
                    } else {
                        //noinspection unchecked
                        stmt = (E) nativeJdbcExtractor.getNativeStatement(stmt);
                    }
                }

                return action.execute(stmt);
            } catch (Exception e) {
                release(stmt);
                throw e;
            }
        });
    }


    public <E> int[] executeBatch(String sql, Stream<E> input, StatementMapper<E> pse) {
        return execute(statement -> {
            try {
                Iterable<E> iterator = input::iterator;
                for (E e : iterator) {
                    if (pse != null)
                        pse.prepare(statement, e);
                    statement.addBatch();
                }
            } finally {

                input.close();
            }
            return statement.executeBatch();
        }, con -> con.prepareStatement(sql));
    }

    public <E> int executeUpdate(String sql, E input, StatementMapper<E> pse) {
        return execute(statement -> {
            if (pse != null)
                pse.prepare(statement, input);
            return statement.executeUpdate();
        }, con -> con.prepareStatement(sql));
    }

    public int executeUpdate(String sql) {
        return executeUpdate(sql, null, null);
    }

    public <E> Stream<E> executeStreamInsertGenerated(String sql, Stream<E> input, StatementMapper<E> pse, Function<E, RowMapper<E>> keySetter) {
        return executeDontClose(statement -> {
            Connection connection = statement.getConnection();
            return input.onClose(() -> {
                release(statement);
                release(connection);
            }).map(it -> translateExceptions("StreamInsertMapping", sql, () -> insertAndGetKey(statement, it, pse, keySetter)));
        }, con -> con.prepareStatement(sql));
    }

    public <E> E executeInsertGenerated(String sql, E input, StatementMapper<E> pse, Function<E, RowMapper<E>> keySetter) {
        return execute(statement -> insertAndGetKey(statement, input, pse, keySetter), con -> con.prepareStatement(sql));
    }

    protected <E> E insertAndGetKey(PreparedStatement statement, E input, StatementMapper<E> pse, Function<E, RowMapper<E>> keySetter) throws SQLException {
        if (pse != null)
            pse.prepare(statement, input);
        statement.executeUpdate();
        if (keySetter != null) {
            ResultSet rs = statement.getGeneratedKeys();
            try {
                if (rs.next())
                    keySetter.apply(input).mapRow(rs);
            } finally {
                release(rs);
            }
        }
        return input;
    }

    public <E, F> Stream<F> executeQuery(String sql, E input, StatementMapper<E> pse, RowMapper<F> rowMapper) {
        return executeDontClose(statement -> {
            if (pse != null)
                pse.prepare(statement, input);
            ResultSet rs = statement.executeQuery();
            Connection connection = statement.getConnection();
            return StreamSupport.stream(Spliterators.spliteratorUnknownSize(new ResultSetIterator(rs, sql), 0), false).onClose(() -> {
                release(rs);
                release(statement);
                release(connection);
            }).map(it -> translateExceptions("StreamMapping", sql, () -> rowMapper.mapRow(it)));
        }, con -> con.prepareStatement(sql));
    }

    public UpdateQuery createQuery(String sql) {
        return new UpdateQuery(sql, this);
    }

    public <E> SelectQuery<E> createQuery(String sql, RowMapper<E> mapper) {
        return new SelectQuery<>(sql, this, mapper);
    }

    public <E> SelectQuery<E> createQuery(String sql, Class<E> mappingType) {
        return new SelectQuery<>(sql, this, getMappingData(mappingType).getMapper());
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        try {
            transactionTemplate = applicationContext.getBean(TransactionTemplate.class);
        } catch (NoSuchBeanDefinitionException e) {
            log.warn("No transaction template has been declared! Creating one based on the data source.");
            transactionTemplate = new TransactionTemplate(new DataSourceTransactionManager(getDataSource()));
        }
    }

    public TransactionTemplate getTransactionTemplate() {
        return transactionTemplate;
    }

    public NamingConvention getNamingConvention() {
        return namingConvention;
    }

    protected interface JdbcCallback<E> {
        E execute() throws SQLException;
    }

    protected <E> E translateExceptions(String task, String sql, JdbcCallback<E> callback) {
        try {
            return callback.execute();
        } catch (SQLException e) {
            throw getExceptionTranslator().translate(task, sql, e);
        }
    }


    protected void release(Statement st) {
        JdbcUtils.closeStatement(st);
    }

    protected void release(ResultSet st) {
        JdbcUtils.closeResultSet(st);
    }

    protected void release(Connection con) {
        DataSourceUtils.releaseConnection(con, getDataSource());
    }

    public class ResultSetIterator implements Iterator<ResultSet> {

        private ResultSet rs;
        private String sql;

        public ResultSetIterator(ResultSet rs, String sql) {
            this.rs = rs;
            this.sql = sql;
        }


        @Override
        public boolean hasNext() {
            return translateExceptions("StreamIteration", sql, rs::next);
        }

        @Override
        public ResultSet next() {
            return rs;
        }
    }
}
