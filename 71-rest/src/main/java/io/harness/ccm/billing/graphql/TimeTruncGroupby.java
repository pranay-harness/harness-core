package io.harness.ccm.billing.graphql;

import io.harness.ccm.billing.bigquery.TruncExpression;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TimeTruncGroupby {
  TruncExpression.DatePart resolution;
  DbColumn entity;
  String alias;

  public Object toGroupbyObject() {
    return new TruncExpression(entity, resolution, alias);
  }
}
