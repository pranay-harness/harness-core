package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;
import software.wings.settings.UsageRestrictions;
import software.wings.yaml.setting.VerificationProviderYaml;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@JsonTypeName("DATA_DOG")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"apiKey", "applicationKey"})
public class DatadogConfig extends SettingValue implements Encryptable {
  public static final String validationUrl = "metrics";

  @Attributes(title = "URL", required = true) @NotEmpty private String url;

  @Attributes(title = "API Key", required = true) @Encrypted private char[] apiKey;

  @Attributes(title = "Application Key", required = true) @Encrypted private char[] applicationKey;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApiKey;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedApplicationKey;

  public DatadogConfig() {
    super(SettingVariableTypes.DATA_DOG.name());
  }

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP2"})
  public DatadogConfig(String url, char[] apiKey, char[] applicationKey, String accountId, String encryptedApiKey,
      String encryptedApplicationKey) {
    this();
    this.url = url;
    this.apiKey = apiKey;
    this.applicationKey = applicationKey;
    this.accountId = accountId;
    this.encryptedApiKey = encryptedApiKey;
    this.encryptedApplicationKey = encryptedApplicationKey;
  }

  private Map<String, String> optionsMap() {
    Map<String, String> paramsMap = new HashMap<>();
    paramsMap.put("api_key", new String(apiKey));
    paramsMap.put("application_key", new String(applicationKey));
    paramsMap.put("from", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    paramsMap.put("to", String.valueOf(System.currentTimeMillis() / TimeUnit.SECONDS.toMillis(1)));
    return paramsMap;
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig() {
    return APMValidateCollectorConfig.builder()
        .baseUrl(url)
        .url(validationUrl)
        .options(optionsMap())
        .headers(new HashMap<>())
        .build();
  }

  @Data
  @EqualsAndHashCode(callSuper = true)
  @NoArgsConstructor
  public static final class DatadogYaml extends VerificationProviderYaml {
    private String url;
    private String apiKey;
    private String applicationKey;

    @Builder
    public DatadogYaml(String type, String harnessApiVersion, String url, String apiKey, String applicationKey,
        UsageRestrictions usageRestrictions) {
      super(type, harnessApiVersion, usageRestrictions);
      this.url = url;
      this.apiKey = apiKey;
      this.applicationKey = applicationKey;
    }
  }
}
