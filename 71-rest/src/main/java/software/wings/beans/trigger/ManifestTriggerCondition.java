package software.wings.beans.trigger;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import lombok.Builder;
import lombok.Data;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

@OwnedBy(HarnessTeam.CDC)
@JsonTypeName("NEW_MANIFEST")
@Data
@Builder
public class ManifestTriggerCondition extends TriggerCondition {
  @NotEmpty private String appManifestId;
  private String serviceId;
  @Transient private String serviceName;
  private String versionRegex;

  public ManifestTriggerCondition() {
    super(TriggerConditionType.NEW_MANIFEST);
  }

  public ManifestTriggerCondition(String appManifestId, String serviceId, String serviceName, String versionRegex) {
    this();
    this.appManifestId = appManifestId;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.versionRegex = versionRegex;
  }
}
