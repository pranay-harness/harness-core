/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.core.ssh.executors;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.FileBucket;
import io.harness.logging.CommandExecutionStatus;

import software.wings.beans.artifact.ArtifactStreamAttributes;
import software.wings.beans.command.CopyConfigCommandUnit;

import java.util.List;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
public interface FileBasedScriptExecutor {
  CommandExecutionStatus copyConfigFiles(CopyConfigCommandUnit.ConfigFileMetaData configFileMetaData);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, List<String> files);

  CommandExecutionStatus copyFiles(String destinationDirectoryPath, ArtifactStreamAttributes artifactStreamAttributes,
      String accountId, String appId, String activityId, String commandUnitName, String hostName);

  CommandExecutionStatus copyGridFsFiles(
      String destinationDirectoryPath, FileBucket fileBucket, List<Pair<String, String>> fileNamesIds);
}
