package software.wings.beans.s3;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(HarnessTeam.CDP)
@TargetModule(HarnessModule._950_DELEGATE_TASKS_BEANS)
public class S3File {
  private String fileKey;
  private String fileContent;
}
