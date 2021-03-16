package software.wings.delegatetasks;

import io.harness.annotations.dev.BreakDependencyOn;
import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.delegate.beans.DelegateFileManagerBase;
import io.harness.delegate.beans.FileBucket;

import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import javax.validation.constraints.NotNull;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by rishi on 12/19/16.
 */
@TargetModule(Module._950_DELEGATE_TASKS_BEANS)
@BreakDependencyOn("io.harness.delegate.beans.DelegateAgentFileService")
@BreakDependencyOn("software.wings.beans.artifact.ArtifactStreamAttributes")
public interface DelegateFileManager extends DelegateFileManagerBase {
  // TODO: this method does not seem to belong here
  InputStream downloadArtifactByFileId(@NotNull FileBucket bucket, @NotEmpty String fileId, @NotEmpty String accountId)
      throws IOException, ExecutionException;

  InputStream downloadArtifactAtRuntime(ArtifactStreamAttributes artifactStreamAttributes, String accountId,
      String appId, String activityId, String commandUnitName, String hostName) throws IOException, ExecutionException;

  Long getArtifactFileSize(ArtifactStreamAttributes artifactStreamAttributes);

  void deleteCachedArtifacts();
}
