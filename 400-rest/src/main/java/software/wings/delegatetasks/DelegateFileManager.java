/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.delegatetasks;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;

import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(HarnessTeam.DEL)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
@BreakDependencyOn("software.wings.beans.artifact.ArtifactStreamAttributes")
public interface DelegateFileManager extends DelegateFileManagerBase {
  InputStream downloadArtifactByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException, ExecutionException;

  InputStream downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) throws IOException, ExecutionException;

  Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes);

  void deleteCachedArtifacts();
}
