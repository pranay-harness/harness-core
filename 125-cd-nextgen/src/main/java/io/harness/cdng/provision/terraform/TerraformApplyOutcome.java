package io.harness.cdng.provision.terraform;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pms.sdk.core.data.Outcome;

import java.util.HashMap;
import java.util.Map;

@OwnedBy(HarnessTeam.CDP)
public class TerraformApplyOutcome extends HashMap<String, Object> implements Outcome {
  public TerraformApplyOutcome(Map<String, ?> m) {
    super(m);
  }
}
