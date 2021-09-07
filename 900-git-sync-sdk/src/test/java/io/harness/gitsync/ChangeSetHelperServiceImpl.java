package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gittoharness.GitSdkInterface;

import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(DX)
public class ChangeSetHelperServiceImpl implements GitSdkInterface {
  @Override
  public void process(ChangeSet changeSet) {}

  @Override
  public boolean markEntityInvalid(String accountId, EntityInfo entityInfo) {
    return false;
  }
}