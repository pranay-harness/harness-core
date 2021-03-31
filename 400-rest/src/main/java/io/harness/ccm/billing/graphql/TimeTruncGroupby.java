package io.harness.ccm.billing.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.billing.bigquery.TruncExpression;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
public class TimeTruncGroupby {
  TruncExpression.DatePart resolution;
  DbColumn entity;
  String alias;

  public Object toGroupbyObject() {
    return new TruncExpression(entity, resolution, alias);
  }
}
