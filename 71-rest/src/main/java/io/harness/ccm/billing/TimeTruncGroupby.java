package io.harness.ccm.billing;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import io.harness.ccm.billing.bigquery.TruncExpression;
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
