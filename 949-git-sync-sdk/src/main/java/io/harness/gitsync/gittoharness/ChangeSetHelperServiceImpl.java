package io.harness.gitsync.gittoharness;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.ChangeSet;

import com.google.inject.Singleton;

@Singleton
@OwnedBy(DX)
public class ChangeSetHelperServiceImpl implements ChangeSetHelperService {
  @Override
  public void process(ChangeSet changeSet) {}
}
