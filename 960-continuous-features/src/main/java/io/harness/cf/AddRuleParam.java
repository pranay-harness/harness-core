/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
