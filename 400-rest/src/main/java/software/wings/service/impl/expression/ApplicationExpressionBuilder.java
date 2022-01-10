/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static java.util.Arrays.asList;

import io.harness.annotations.dev.OwnedBy;

import java.util.Set;
import java.util.TreeSet;

@OwnedBy(CDC)
public class ApplicationExpressionBuilder extends ExpressionBuilder {
  @Override
  public Set<String> getExpressions(String appId, String entityId) {
    Set<String> expressions = new TreeSet<>();
    expressions.addAll(asList(APP_NAME, APP_DESCRIPTION));
    expressions.addAll(asList(ENV_NAME, ENV_DESCRIPTION));
    expressions.addAll(asList(SERVICE_NAME, SERVICE_DESCRIPTION));
    expressions.addAll(asList(WORKFLOW_NAME, WORKFLOW_DESCRIPTION));
    expressions.addAll(asList(PIPELINE_NAME, PIPELINE_DESCRIPTION));
    return expressions;
  }

  @Override
  public Set<String> getDynamicExpressions(String appId, String entityId) {
    return null;
  }
}
