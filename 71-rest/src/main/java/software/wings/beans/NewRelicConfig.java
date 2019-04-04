package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.newrelic.NewRelicUrlProvider;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.yaml.setting.VerificationProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by raghu on 8/28/17.
 */
@JsonTypeName("NEW_RELIC")
@Data
@Builder
@ToString(exclude = "apiKey")
@EqualsAndHashCode(callSuper = false)
public class NewRelicConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @EnumData(enumDataProvider = NewRelicUrlProvider.class)
  @Attributes(title = "URL")
  @NotEmpty
  @DefaultValue("https://api.newrelic.com")
  private String newRelicUrl = "https://api.newrelic.com";

  @Attributes(title = "API key", required = true) @Encrypted private char[] apiKey;

  @Attributes(title = "NewRelic Account Id") private String newRelicAccountId;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiKey;

  /**
   * Instantiates a new New Relic dynamics config.
   */
  public NewRelicConfig() {
    super(StateType.NEW_RELIC.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NewRelicConfig(
      String newRelicUrl, char[] apiKey, String newRelicAccountId, String accountId, String encryptedApiKey) {
    this();
    this.newRelicUrl = newRelicUrl;
    this.apiKey = apiKey;
    this.accountId = accountId;
    this.newRelicAccountId = newRelicAccountId;
    this.encryptedApiKey = encryptedApiKey;
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NewRelicConfig(String newRelicUrl, char[] apiKey, String accountId, String encryptedApiKey) {
    this();
    this.newRelicUrl = newRelicUrl;
    this.apiKey = apiKey;
    this.accountId = accountId;
    this.newRelicAccountId = "";
    this.encryptedApiKey = encryptedApiKey;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(newRelicUrl));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class Yaml extends VerificationProviderYaml {
    private String apiKey;
    private String newRelicAccountId;

    @Builder
    public Yaml(String type, String harnessApiVersion, String apiKey, String newRelicAccountId,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.apiKey = apiKey;
      this.newRelicAccountId = newRelicAccountId;
    }
  }
}
