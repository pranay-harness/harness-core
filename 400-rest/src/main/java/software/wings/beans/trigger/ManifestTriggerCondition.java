package software.wings.beans.trigger;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldNameConstants;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;

@OwnedBy(HarnessTeam.CDC)
@JsonTypeName("NEW_MANIFEST")
@Data
@Builder
@FieldNameConstants(innerTypeName = "ManifestTriggerConditionKeys")
@TargetModule(HarnessModule._815_CG_TRIGGERS)
public class ManifestTriggerCondition extends TriggerCondition {
  @NotEmpty private String appManifestId;
  private String serviceId;
  @Transient private String serviceName;
  private String versionRegex;
  private String appManifestName;

  public ManifestTriggerCondition() {
    super(TriggerConditionType.NEW_MANIFEST);
  }

  public ManifestTriggerCondition(
      String appManifestId, String serviceId, String serviceName, String versionRegex, String appManifestName) {
    this();
    this.appManifestId = appManifestId;
    this.serviceId = serviceId;
    this.serviceName = serviceName;
    this.versionRegex = versionRegex;
    this.appManifestName = appManifestName;
  }
}
