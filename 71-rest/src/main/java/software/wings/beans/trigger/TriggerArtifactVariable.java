package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.beans.EntityType;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "ArtifactSelectionKeys")
public class TriggerArtifactVariable {
  @NotEmpty private String variableName;

  @NotEmpty private String entityId;
  @NotEmpty private EntityType entityType;
  private transient String entityName;

  private TriggerArtifactSelectionValue variableValue;

  //  @Data
  //  @NoArgsConstructor
  //  @EqualsAndHashCode(callSuper = true)
  //  public static final class Yaml extends BaseYaml {
  //    String type;
  //    private String artifactStreamName;
  //    private boolean regex;
  //    private String artifactFilter;
  //    String workflowName;
  //    String serviceName;
  //
  //    @lombok.Builder
  //    public Yaml(String type, String artifactStreamName, String workflowName, String artifactFilter, String
  //    serviceName,
  //        boolean regex) {
  //      this.artifactStreamName = artifactStreamName;
  //      this.workflowName = workflowName;
  //      this.artifactFilter = artifactFilter;
  //      this.type = type;
  //      this.regex = regex;
  //      this.serviceName = serviceName;
  //    }
  //  }
}
