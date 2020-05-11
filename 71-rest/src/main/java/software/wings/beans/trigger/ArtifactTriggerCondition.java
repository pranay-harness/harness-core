package software.wings.beans.trigger;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static software.wings.beans.trigger.TriggerConditionType.NEW_ARTIFACT;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;

/**
 * Created by sgurubelli on 10/25/17.
 */
@OwnedBy(CDC)
@JsonTypeName("NEW_ARTIFACT")
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@FieldNameConstants(innerTypeName = "ArtifactTriggerConditionKeys")
public class ArtifactTriggerCondition extends TriggerCondition {
  @NotEmpty private String artifactStreamId;
  private String artifactSourceName;
  private String artifactFilter;
  private boolean regex;

  public ArtifactTriggerCondition() {
    super(NEW_ARTIFACT);
  }

  public ArtifactTriggerCondition(
      String artifactStreamId, String artifactSourceName, String artifactFilter, boolean regex) {
    this();
    this.artifactStreamId = artifactStreamId;
    this.artifactSourceName = artifactSourceName;
    this.artifactFilter = artifactFilter;
    this.regex = regex;
  }
}
