package io.harness.gitsync;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gittoharness.ChangeSetHelperService;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class ChangeSetHelperServiceImpl implements ChangeSetHelperService {
  @Override
  public void process(ChangeSet changeSet) {}
}
