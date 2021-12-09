package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.service.intfc.DelegateUpgraderService;

import com.google.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.math3.util.Pair;

@Singleton
@ValidateOnExecution
@Slf4j
@OwnedBy(HarnessTeam.DEL)
public class DelegateUpgraderServiceImpl implements DelegateUpgraderService {
  private static final String DELEGATEIMAGETAG = "harness/delegate:latest";

  @Override
  public Pair<Boolean, String> getDelegateImageTag(String accountId, String currentDelegateImageTag) {
    return new Pair<>(false, DELEGATEIMAGETAG);
  }
}
