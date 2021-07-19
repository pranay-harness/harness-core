package io.harness.cf;

import io.harness.cf.openapi.model.Clause;
import io.harness.cf.openapi.model.Serve;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

class AddRuleParam {
  List<Clause> clauses = new ArrayList<>();
  int priority;
  io.harness.cf.openapi.model.Serve serve;
  String uuid;

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
