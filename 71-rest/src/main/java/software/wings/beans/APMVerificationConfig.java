package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.exception.WingsException;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.states.APMVerificationState.Method;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("APM_VERIFICATION")
@Data
@ToString(exclude = {"headersList", "optionsList"})
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class APMVerificationConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Transient @SchemaIgnore private static final String MASKED_STRING = "*****";

  @Attributes(title = "Base Url") private String url;

  @NotEmpty @Attributes(title = "Validation Url", required = true) private String validationUrl;

  private String validationBody;

  private Method validationMethod;

  @SchemaIgnore private boolean logVerification;

  @SchemaIgnore @NotEmpty private String accountId;

  private List<KeyValues> headersList;
  private List<KeyValues> optionsList;

  /**LogMLAnalysisRecord.java
   * Instantiates a new config.
   */
  public APMVerificationConfig() {
    super(SettingVariableTypes.APM_VERIFICATION.name());
  }

  public APMVerificationConfig(SettingVariableTypes type) {
    super(type.name());
  }

  public String getValidationUrl() {
    if (isEmpty(validationUrl)) {
      return validationUrl;
    }
    try {
      return validationUrl.replaceAll("`", URLEncoder.encode("`", "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      throw new WingsException("Unsupported encoding exception while encoding backticks in " + validationUrl);
    }
  }

  public Map<String, String> collectionHeaders() {
    Map<String, String> headers = new HashMap<>();
    if (!isEmpty(headersList)) {
      for (KeyValues keyValue : headersList) {
        if (keyValue.encrypted) {
          headers.put(keyValue.getKey(), "${" + keyValue.getKey() + "}");
        } else {
          headers.put(keyValue.getKey(), keyValue.getValue());
        }
      }
    }
    return headers;
  }

  public Map<String, String> collectionParams() {
    Map<String, String> params = new HashMap<>();
    if (!isEmpty(optionsList)) {
      for (KeyValues keyValue : optionsList) {
        if (keyValue.encrypted) {
          params.put(keyValue.getKey(), "${" + keyValue.getKey() + "}");
        } else {
          params.put(keyValue.getKey(), keyValue.getValue());
        }
      }
    }
    return params;
  }

  public APMValidateCollectorConfig createAPMValidateCollectorConfig(
      SecretManager secretManager, EncryptionService encryptionService) {
    try {
      Map<String, String> headers = new HashMap<>();
      if (!isEmpty(headersList)) {
        for (KeyValues keyValue : headersList) {
          if (keyValue.encrypted && MASKED_STRING.equals(keyValue.value)) {
            headers.put(keyValue.getKey(),
                new String(encryptionService.getDecryptedValue(
                    secretManager.encryptedDataDetails(accountId, keyValue.key, keyValue.encryptedValue).get())));
          } else {
            headers.put(keyValue.getKey(), keyValue.getValue());
          }
        }
      }

      Map<String, String> options = new HashMap<>();
      if (!isEmpty(optionsList)) {
        for (KeyValues keyValue : optionsList) {
          if (keyValue.encrypted && MASKED_STRING.equals(keyValue.value)) {
            options.put(keyValue.getKey(),
                new String(encryptionService.getDecryptedValue(
                    secretManager.encryptedDataDetails(accountId, keyValue.key, keyValue.encryptedValue).get())));
          } else {
            options.put(keyValue.getKey(), keyValue.getValue());
          }
        }
      }

      return APMValidateCollectorConfig.builder()
          .baseUrl(url)
          .url(validationUrl)
          .body(validationBody)
          .collectionMethod(validationMethod)
          .headers(headers)
          .options(options)
          .build();
    } catch (Exception ex) {
      throw new WingsException("Unable to validate connector ", ex);
    }
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(
        HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(getValidationUrl()));
  }

  @Data
  @Builder
  public static class KeyValues {
    private String key;
    private String value;
    private boolean encrypted;
    private String encryptedValue;
  }

  public List<EncryptedDataDetail> encryptedDataDetails(SecretManager secretManager) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    if (headersList != null) {
      encryptedDataDetails.addAll(
          headersList.stream()
              .filter(entry -> entry.encrypted)
              .map(entry
                  -> secretManager.encryptedDataDetails(accountId, entry.key, entry.encryptedValue)
                         .<WingsException>orElseThrow(() -> new WingsException("Unable to decrypt field " + entry.key)))
              .collect(Collectors.toList()));
    }
    if (optionsList != null) {
      encryptedDataDetails.addAll(
          optionsList.stream()
              .filter(entry -> entry.encrypted)
              .map(entry
                  -> secretManager.encryptedDataDetails(accountId, entry.key, entry.encryptedValue)
                         .<WingsException>orElseThrow(() -> new WingsException("Unable to decrypt field " + entry.key)))
              .collect(Collectors.toList()));
    }

    return encryptedDataDetails.size() > 0 ? encryptedDataDetails : null;
  }

  // TODO won't work for vault
  public void encryptFields(SecretManager secretManager) {
    if (headersList != null) {
      headersList.stream()
          .filter(header -> header.encrypted)
          .filter(header -> !header.value.equals(MASKED_STRING))
          .forEach(header -> {
            header.encryptedValue = secretManager.encrypt(accountId, header.value, null);
            header.value = MASKED_STRING;
          });
    }

    if (optionsList != null) {
      optionsList.stream()
          .filter(option -> option.encrypted)
          .filter(option -> !option.value.equals(MASKED_STRING))
          .forEach(option -> {
            option.encryptedValue = secretManager.encrypt(accountId, option.value, null);
            option.value = MASKED_STRING;
          });
    }
  }
}
