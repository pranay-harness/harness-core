package io.harness.service.intfc;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import org.apache.commons.math3.util.Pair;

@OwnedBy(HarnessTeam.DEL)
public interface DelegateUpgraderService {
  Pair<Boolean, String> getDelegateImageTag(String accountId, String currentDelegateImageTag);
}
