package fr.petitl.relational.repository.template.query;

import java.util.Arrays;

import fr.petitl.relational.repository.template.RelationalTemplate;

/**
 *
 */
public class BatchUpdateQuery extends AbstractQuery<BatchUpdateQuery> {
    private int maxBatch;
    private int n = 0;
    private int currentTotal = 0;
    private RelationalTemplate.BatchOperations batch;

    public BatchUpdateQuery(String sql, RelationalTemplate template) {
        super(sql, template);
        if (!query.isStatic()) {
            throw new IllegalArgumentException("Query contains non-static element like preprocessors and can't be applied to batching");
        }
        maxBatch = template.getMaxBatch();
    }

    /**
     * Adds the current setParameters to the batch (i.e. in the prepared statement)
     * Can call an execute batch to flush data if the number of calls is higher than the maxBatch
     */
    public void next() {
        if (batch == null) {
            batch = template.batch(query.getQueryString());
        }
        batch.prepare(query);
        query.clear();
        if (++n == maxBatch) {
            execute();
        }
    }

    /**
     * Must be called to release resources.
     * <p>
     * Flush the batch (or the rest of the batch) to the database and release resources.
     *
     * @return The number of changes operated in the database
     */
    public int finish() {
        // If nothing is done, nothing will be done
        if (n == 0) return 0;
        execute();
        this.n = 0;
        int result = this.currentTotal;
        currentTotal = 0;
        batch.clean();
        batch = null;
        return result;
    }

    public void setMaxBatch(int maxBatch) {
        this.maxBatch = maxBatch;
    }

    protected int execute() {
        int[] batchResult = batch.execute();
        currentTotal += Arrays.stream(batchResult).sum();
        return currentTotal;
    }
}
