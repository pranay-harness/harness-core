package io.harness.service.impl;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;

@OwnedBy(HarnessTeam.DEL)
public class DelegateTokenServiceImpl {
  @Inject private HPersistence persistence;
}
