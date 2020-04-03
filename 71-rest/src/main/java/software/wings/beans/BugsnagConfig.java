package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.data.structure.EmptyPredicate;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.encryption.Encrypted;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@JsonTypeName("BUG_SNAG")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class BugsnagConfig extends SettingValue implements EncryptableSetting {
  public static final String validationUrl = "user/organizations";

  @Attributes(title = "URL", required = true) @NotEmpty private String url;

  @Attributes(title = "Auth Token", required = true) @Encrypted private char[] authToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedAuthToken;

  public BugsnagConfig() {
    super(SettingVariableTypes.BUG_SNAG.name());
  }

  private BugsnagConfig(String url, char[] authToken, String accountId, String encryptedAuthToken) {
    this();
    this.url = url;
    this.authToken = authToken;
    this.accountId = accountId;
    this.encryptedAuthToken = encryptedAuthToken;
  }

  public Map<String, String> optionsMap() {
    return new HashMap<>();
  }

  public Map<String, String> headersMap() {
    Map<String, String> headerMap = new HashMap<>();
    if (!EmptyPredicate.isEmpty(authToken)) {
      headerMap.put("Authorization", "token " + new String(authToken));
    } else {
      headerMap.put("Authorization", "token ${authToken}");
    }
    return headerMap;
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig() {
    return APMValidateCollectorConfig.builder()
        .baseUrl(url)
        .url(validationUrl)
        .options(optionsMap())
        .headers(headersMap())
        .build();
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getUrl() + validationUrl));
  }
}
