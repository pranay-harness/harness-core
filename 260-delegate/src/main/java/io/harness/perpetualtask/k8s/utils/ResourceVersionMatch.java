package io.harness.perpetualtask.k8s.utils;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

@TargetModule(HarnessModule._420_DELEGATE_AGENT)
@OwnedBy(HarnessTeam.CE)
public interface ResourceVersionMatch {
  String MOST_RECENT = "MostRecent";
  String EXACT = "Exact";
  String NOT_OLDER_THAN = "NotOlderThan";
}
