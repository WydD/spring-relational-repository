package fr.petitl.relational.repository.dialect;

import fr.petitl.relational.repository.dialect.generic.CompositeAsTupleSQLGeneration;
import fr.petitl.relational.repository.dialect.paging.*;
import fr.petitl.relational.repository.query.macro.SingleInMacro;

/**
 *
 */
public class SimpleDialectProvider {
    public static BeanDialect h2() {
        return new BeanDialect(new LimitPaging())
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect mysql() {
        return new BeanDialect(new LimitPaging())
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect postgresql() {
        return new BeanDialect(CompositeAsTupleSQLGeneration::new, new FetchFirstPaging(true))
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect oracle12() {
        return standardSql2008();
    }

    public static BeanDialect oracle() {
        return new BeanDialect(new RowNumPaging())
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect db2() {
        return new BeanDialect(new OffsetAlternativePaging(new FetchFirstPaging(false), new WindowPaging.RowNumber()))
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect standardSql2008() {
        return new BeanDialect(new FetchFirstPaging(true))
                .addMacro(new SingleInMacro());
    }

    public static BeanDialect standardSql2003() {
        return new BeanDialect(new WindowPaging.RowNumber())
                .addMacro(new SingleInMacro());
    }
}
