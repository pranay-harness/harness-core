package io.harness.yaml.extended.ci.codebase;

import static io.harness.annotations.dev.HarnessTeam.CI;

import io.harness.annotations.dev.OwnedBy;

@OwnedBy(CI)
public class BuildTypeConstants {
  public static final String BRANCH = "branch";
  public static final String TAG = "tag";
  public static final String PR = "PR";
  public static final String COMMIT_SHA = "CommitSha";
}
