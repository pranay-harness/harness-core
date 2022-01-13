/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.graphql;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.billing.bigquery.TruncExpression;

import com.healthmarketscience.sqlbuilder.dbspec.basic.DbColumn;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CE)
@TargetModule(HarnessModule._375_CE_GRAPHQL)
public class TimeTruncGroupby {
  TruncExpression.DatePart resolution;
  DbColumn entity;
  String alias;

  public Object toGroupbyObject() {
    return new TruncExpression(entity, resolution, alias);
  }
}
