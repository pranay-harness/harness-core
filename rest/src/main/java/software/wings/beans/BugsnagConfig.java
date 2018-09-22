package software.wings.beans;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.harness.data.structure.EmptyPredicate;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("BUG_SNAG")
@Data
@Builder
@EqualsAndHashCode(callSuper = false)
public class BugsnagConfig extends SettingValue implements Encryptable {
  public static final String validationUrl = "user/organizations";

  @Attributes(title = "URL", required = true) @NotEmpty private String url;

  @Attributes(title = "Auth Token", required = true) @Encrypted private char[] authToken;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedAuthToken;

  public BugsnagConfig() {
    super(SettingVariableTypes.BUG_SNAG.name());
  }

  @SuppressFBWarnings({"EI_EXPOSE_REP2", "EI_EXPOSE_REP2"})
  public BugsnagConfig(String url, char[] authToken, String accountId, String encryptedAuthToken) {
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
}
