package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

@OwnedBy(CDC)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldNameConstants(innerTypeName = "ManifestSelectionKeys")
public class ManifestSelection {
  @NotEmpty private String serviceId;
  private String serviceName;
  @NotEmpty private ManifestSelectionType type;
  private String appManifestId;
  private String versionRegex;
  private String pipelineId;
  private String pipelineName;
  private String workflowId;
  private String workflowName;

  public enum ManifestSelectionType {
    FROM_APP_MANIFEST,
    LAST_COLLECTED,
    LAST_DEPLOYED,
    PIPELINE_SOURCE,
    WEBHOOK_VARIABLE
  }
}
