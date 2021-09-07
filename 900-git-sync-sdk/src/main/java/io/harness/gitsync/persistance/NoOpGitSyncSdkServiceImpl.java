package io.harness.gitsync.persistance;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject }))
@Slf4j
@OwnedBy(DX)
public class NoOpGitSyncSdkServiceImpl implements GitSyncSdkService {
  @Override
  public boolean isGitSyncEnabled(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return false;
  }

  @Override
  public boolean isDefaultBranch(String accountIdentifier, String orgIdentifier, String projectIdentifier) {
    return false;
  }
}
