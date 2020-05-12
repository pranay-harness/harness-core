package software.wings.beans;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.audit.ResourceType;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.ArtifactServerYaml;

import java.util.List;

@OwnedBy(CDC)
@JsonTypeName("CUSTOM")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class CustomArtifactServerConfig extends SettingValue {
  @SchemaIgnore @NotEmpty private String accountId;

  public CustomArtifactServerConfig() {
    super(SettingVariableTypes.CUSTOM.name());
  }

  public CustomArtifactServerConfig(String accountId) {
    this();
    this.accountId = accountId;
  }
  @Override
  public String fetchResourceCategory() {
    return ResourceType.ARTIFACT_SERVER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return null;
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends ArtifactServerYaml {
    @Builder
    public Yaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, null, null, null, usageRestrictions);
    }
  }
}
