/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.openapi.model.Clause;
import io.harness.cf.openapi.model.Serve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@OwnedBy(HarnessTeam.CF)
class AddRuleParam {
  List<Clause> clauses = new ArrayList<>();
  int priority;
  io.harness.cf.openapi.model.Serve serve;
  String uuid;

  public static AddRuleParam newPercentageRollout(String uuid, int priority, Serve serve, List<Clause> clauses) {
    AddRuleParam param = new AddRuleParam();
    if (priority > 0) {
      param.priority = priority;
    }
    param.serve = serve;
    param.uuid = uuid;
    param.clauses = clauses;
    return param;
  }

  public static AddRuleParam getParamsForAccountID(String accountID, int priority) {
    AddRuleParam param = new AddRuleParam();
    param.priority = priority;
    param.serve = new Serve();
    param.serve.variation("true");
    param.uuid = UUID.randomUUID().toString();
    Clause clause = new Clause();
    clause.attribute("identifier");
    clause.op("equal");
    clause.values(Arrays.asList(accountID));
    param.clauses = Arrays.asList(clause);
    return param;
  }
}
