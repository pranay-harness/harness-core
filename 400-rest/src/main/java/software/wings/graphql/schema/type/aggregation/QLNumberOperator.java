/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.schema.type.aggregation;

/**
 * @author rktummala on 06/07/19
 */
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public enum QLNumberOperator {
  EQUALS,
  GREATER_THAN,
  GREATER_THAN_OR_EQUALS,
  IN,
  LESS_THAN,
  LESS_THAN_OR_EQUALS,
  NOT_EQUALS
}
