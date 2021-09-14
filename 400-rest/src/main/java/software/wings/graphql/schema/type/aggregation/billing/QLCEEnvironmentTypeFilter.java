/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.schema.type.aggregation.billing;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.aggregation.Filter;
import software.wings.graphql.schema.type.aggregation.QLIdOperator;

@TargetModule(HarnessModule._375_CE_GRAPHQL)
@OwnedBy(CE)
public class QLCEEnvironmentTypeFilter implements Filter {
  private QLEnvType type;

  @Override
  public QLIdOperator getOperator() {
    return QLIdOperator.EQUALS;
  }

  @Override
  public Object[] getValues() {
    if (type != null) {
      return new String[] {type.toString()};
    }
    return null;
  }
}
