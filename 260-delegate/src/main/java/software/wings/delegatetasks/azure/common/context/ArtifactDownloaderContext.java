package software.wings.delegatetasks.azure.common.context;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.artifact.ArtifactFile;
import software.wings.beans.artifact.ArtifactStreamAttributes;

import java.io.File;
import java.util.List;
import javax.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

@Data
@Builder
@TargetModule(HarnessModule._930_DELEGATE_TASKS)
@OwnedBy(CDP)
public class ArtifactDownloaderContext {
  @NotBlank private String accountId;
  @NotBlank private String appId;
  @NotBlank private String activityId;
  @NotBlank private String commandName;
  @NotNull private ArtifactStreamAttributes artifactStreamAttributes;
  private List<ArtifactFile> artifactFiles;
  @NotNull private File workingDirectory;
}
