package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.annotation.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import ro.fortsoft.pf4j.Extension;
import software.wings.annotation.EncryptableSetting;
import software.wings.jersey.JsonViews;
import software.wings.service.impl.newrelic.NewRelicUrlProvider;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;
import software.wings.stencils.EnumData;
import software.wings.yaml.setting.VerificationProviderYaml;

/**
 * Created by raghu on 8/28/17.
 */
@Extension
@JsonTypeName("NEW_RELIC")
@Data
@EqualsAndHashCode(callSuper = false)
@Builder
@ToString(exclude = "apiKey")
public class NewRelicConfig extends SettingValue implements EncryptableSetting {
  @EnumData(enumDataProvider = NewRelicUrlProvider.class)
  @Attributes(title = "URL")
  @NotEmpty
  @DefaultValue("https://api.newrelic.com")
  private String newRelicUrl = "https://api.newrelic.com";

  @Attributes(title = "API key", required = true) @Encrypted private char[] apiKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiKey;

  /**
   * Instantiates a new New Relic dynamics config.
   */
  public NewRelicConfig() {
    super(StateType.NEW_RELIC.name());
  }

  @SuppressFBWarnings("EI_EXPOSE_REP2")
  public NewRelicConfig(String newRelicUrl, char[] apiKey, String accountId, String encryptedApiKey) {
    this();
    this.newRelicUrl = newRelicUrl;
    this.apiKey = apiKey;
    this.accountId = accountId;
    this.encryptedApiKey = encryptedApiKey;
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class Yaml extends VerificationProviderYaml {
    private String apiKey;

    @Builder
    public Yaml(String type, String harnessApiVersion, String apiKey, UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.apiKey = apiKey;
    }
  }
}
