package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by raghu on 8/28/17.
 */
@Extension
@JsonTypeName("DYNA_TRACE")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "apiToken")
public class DynaTraceConfig extends SettingValue implements Encryptable {
  @Attributes(title = "URL", required = true) private String dynaTraceUrl;

  @Attributes(title = "API Token", required = true)
  @Encrypted
  @JsonView(JsonViews.Internal.class)
  private char[] apiToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @SchemaIgnore private String encryptedApiToken;

  public DynaTraceConfig() {
    super(StateType.DYNA_TRACE.name());
  }

  public DynaTraceConfig(String dynaTraceUrl, char[] apiToken, String accountId, String encryptedApiToken) {
    this();
    this.dynaTraceUrl = dynaTraceUrl;
    this.apiToken = apiToken;
    this.accountId = accountId;
    this.encryptedApiToken = encryptedApiToken;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class DynaTraceYaml extends VerificationProviderYaml {
    private String apiToken;
    private String dynaTraceUrl;

    @Builder
    public DynaTraceYaml(String type, String harnessApiVersion, String dynaTraceUrl, String apiToken,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.dynaTraceUrl = dynaTraceUrl;
      this.apiToken = apiToken;
    }
  }
}
