package io.harness.cf;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cf.openapi.model.Serve;
import io.harness.cf.openapi.model.WeightedVariation;

import java.util.List;

@OwnedBy(HarnessTeam.CF)
class UpdateRuleParam {
  List<WeightedVariation> variations;
  String ruleID;
  String bucketBy;

  public static UpdateRuleParam updatePercentageRollout(String ruleID, Serve serve) {
    UpdateRuleParam param = new UpdateRuleParam();

    param.variations = serve.getDistribution().getVariations();
    param.ruleID = ruleID;
    param.bucketBy = serve.getDistribution().getBucketBy();
    return param;
  }
}