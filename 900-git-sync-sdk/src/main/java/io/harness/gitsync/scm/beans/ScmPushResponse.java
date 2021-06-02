package io.harness.gitsync.scm.beans;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

@OwnedBy(HarnessTeam.DX)
/**
 * Interface to capture all push events from scm.
 * Push events can be of types -
 * <li>Create file</li>
 * <li>Update file</li>
 * <li>Delete file</li>
 */
public interface ScmPushResponse {
  String getFilePath();

  boolean isPushToDefaultBranch();

  String getYamlGitConfigId();

  String getObjectId();

  String getFolderPath();

  String getBranch();
}
