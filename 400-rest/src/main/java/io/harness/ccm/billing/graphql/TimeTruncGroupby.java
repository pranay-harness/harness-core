package io.harness.ccm.billing.graphql;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.bigquery.TruncExpression;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@TargetModule(Module._490_CE_COMMONS)
public class TimeTruncGroupby {
  TruncExpression.DatePart resolution;
  DbColumn entity;
  String alias;

  public Object toGroupbyObject() {
    return new TruncExpression(entity, resolution, alias);
  }
}
