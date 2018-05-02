package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.annotation.Encryptable;
import software.wings.annotation.Encrypted;
import software.wings.exception.WingsException;
import software.wings.jersey.JsonViews;
import software.wings.settings.SettingValue;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("APM_VERIFICATION")
@Data
@EqualsAndHashCode(callSuper = false)
public class APMVerificationConfig extends SettingValue implements Encryptable {
  @Attributes(title = "Base Url") private String url;

  @Attributes(title = "Headers", description = "<key>:<value> one per line") @Encrypted private char[] headers;

  @Attributes(title = "Options", description = "<key>:<value> one per line") @Encrypted private char[] options;

  @NotEmpty @Attributes(title = "Validation Url", required = true) private String validationUrl;

  @SchemaIgnore @NotEmpty private String accountId;

  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedHeaders;
  @JsonView(JsonViews.Internal.class) @SchemaIgnore private String encryptedOptions;

  /**
   * Instantiates a new config.
   */
  public APMVerificationConfig() {
    super(SettingVariableTypes.APM_VERIFICATION.name());
  }

  public APMVerificationConfig(SettingVariableTypes type) {
    super(type.name());
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig() {
    return APMValidateCollectorConfig.builder()
        .baseUrl(url)
        .url(validationUrl)
        .options(optionsMap())
        .headers(headerMap())
        .build();
  }

  public Map<String, String> headerMap() {
    Map<String, String> headerMap = new HashMap<>();
    if (!isEmpty(headers)) {
      String[] headerList = new String(getHeaders()).split("\\r?\\n");
      for (String header : headerList) {
        String[] headerTokens = header.split(":");
        if (headerTokens.length != 2) {
          throw new WingsException("Unknown header format. Should be <key>:<value> one per line");
        }
        headerMap.put(headerTokens[0], headerTokens[1]);
      }
    }
    return headerMap;
  }

  public Map<String, String> optionsMap() {
    Map<String, String> optionsMap = new HashMap<>();
    if (!isEmpty(options)) {
      String[] optionsList = new String(options).split("\\r?\\n");
      for (String options : optionsList) {
        String[] optionsTokens = options.split(":");
        if (optionsTokens.length != 2) {
          throw new WingsException("Unknown header format. Should be <key>:<value> one per line");
        }
        optionsMap.put(optionsTokens[0], optionsTokens[1]);
      }
    }
    return optionsMap;
  }
}
