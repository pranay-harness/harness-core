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
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

import java.util.Arrays;
import java.util.List;

/**
 * Created by raghu on 8/28/17.
 */
@JsonTypeName("DYNA_TRACE")
@Data
@Builder
@ToString(exclude = "apiToken")
@EqualsAndHashCode(callSuper = false)
public class DynaTraceConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Attributes(title = "URL", required = true) private String dynaTraceUrl;

  @Attributes(title = "API Token", required = true) @Encrypted private char[] apiToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiToken;

  public DynaTraceConfig() {
    super(StateType.DYNA_TRACE.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public DynaTraceConfig(String dynaTraceUrl, char[] apiToken, String accountId, String encryptedApiToken) {
    this();
    this.dynaTraceUrl = dynaTraceUrl;
    this.apiToken = apiToken;
    this.accountId = accountId;
    this.encryptedApiToken = encryptedApiToken;
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(dynaTraceUrl));
  }

  @Data
  @NoArgsConstructor
  @EqualsAndHashCode(callSuper = true)
  public static final class DynaTraceYaml extends VerificationProviderYaml {
    private String apiToken;
    private String dynaTraceUrl;

    @Builder
    public DynaTraceYaml(String type, String harnessApiVersion, String dynaTraceUrl, String apiToken,
        UsageRestrictions.Yaml usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.dynaTraceUrl = dynaTraceUrl;
      this.apiToken = apiToken;
    }
  }
}
