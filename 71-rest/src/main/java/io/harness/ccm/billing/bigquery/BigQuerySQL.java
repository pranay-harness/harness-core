package io.harness.ccm.billing.bigquery;

import com.hazelcast.util.Preconditions;
import com.healthmarketscience.sqlbuilder.Condition;
import com.healthmarketscience.sqlbuilder.SelectQuery;
import com.healthmarketscience.sqlbuilder.custom.postgresql.PgLimitClause;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
public class BigQuerySQL {
  private SelectQuery query;
  private List<Object> selectColumns;
  private List<Object> groupbyObjects;
  private List<Object> sortObjects;
  private List<Condition> conditions;
  private PgLimitClause pgLimit;

  public static class BigQuerySQLBuilder {
    private SelectQuery query = new SelectQuery();
    private List<Object> selectColumns;
    private List<Object> groupbyObjects;
    private List<Object> sortObjects;
    private List<Condition> conditions;
    private PgLimitClause pgLimit;

    public BigQuerySQLBuilder selectColumns(List<Object> selectColumns) {
      Preconditions.checkNotNull(selectColumns);
      this.selectColumns = selectColumns;
      for (Object selectColumn : selectColumns) {
        query.addCustomGroupings(selectColumn);
      }
      return this;
    }

    public BigQuerySQLBuilder groupbyObjects(List<Object> groupbyObjects) {
      Preconditions.checkNotNull(groupbyObjects);
      this.groupbyObjects = groupbyObjects;
      for (Object groupbyObject : groupbyObjects) {
        query.addCustomGroupings(groupbyObject);
      }
      return this;
    }

    public BigQuerySQLBuilder sortObjects(List<Object> sortObjects) {
      Preconditions.checkNotNull(sortObjects);
      this.sortObjects = sortObjects;
      for (Object sortObject : sortObjects) {
        query.addCustomOrderings(sortObject);
      }
      return this;
    }

    public BigQuerySQLBuilder conditions(List<Condition> conditions) {
      Preconditions.checkNotNull(conditions);
      this.conditions = conditions;
      for (Condition condition : conditions) {
        query.addCondition(condition);
      }
      return this;
    }

    public BigQuerySQLBuilder pgLimit(PgLimitClause pgLimit) {
      Preconditions.checkNotNull(pgLimit);
      this.pgLimit = pgLimit;
      query.addCustomization(pgLimit);
      return this;
    }
  }
}
