/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

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
