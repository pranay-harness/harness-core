package software.wings.beans;

import static io.harness.delegate.task.mixin.IgnoreValidationCapabilityGenerator.buildIgnoreValidationCapability;
import static java.util.Collections.singletonList;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.CollaborationProviderYaml;

import java.util.List;

@JsonTypeName("SPOT_INST")
@Data
@Builder
@ToString(exclude = "spotInstToken")
@EqualsAndHashCode(callSuper = false)
public class SpotInstConfig extends SettingValue implements EncryptableSetting {
  @NotEmpty @SchemaIgnore private String accountId;
  private char[] spotInstToken;
  private String spotInstAccountId;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedSpotInstToken;

  public SpotInstConfig() {
    super(SettingVariableTypes.SPOT_INST.name());
  }

  public SpotInstConfig(
      String accountId, char[] spotInstToken, String spotInstAccountId, String encryptedSpotInstToken) {
    this();
    this.accountId = accountId;
    this.spotInstToken = spotInstToken == null ? null : spotInstToken.clone();
    this.spotInstAccountId = spotInstAccountId;
    this.encryptedSpotInstToken = encryptedSpotInstToken;
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.COLLABORATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return singletonList(buildIgnoreValidationCapability());
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends CollaborationProviderYaml {
    private String spotInstToken;
    private String spotInstAccountId;

    @Builder
    public Yaml(String type, String harnessApiVersion, UsageRestrictions.Yaml usageRestrictions, String spotInstToken,
        String spotInstAccountId) {
      super(type, harnessApiVersion, usageRestrictions);
      this.spotInstToken = spotInstToken;
      this.spotInstAccountId = spotInstAccountId;
    }
  }
}