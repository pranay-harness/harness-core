package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator.HttpCapabilityDetailsLevel.QUERY;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.delegate.beans.executioncapability.ExecutionCapability;
import io.harness.delegate.beans.executioncapability.ExecutionCapabilityDemander;
import io.harness.delegate.task.mixin.HttpConnectionExecutionCapabilityGenerator;
import io.harness.eraro.ErrorCode;
import io.harness.exception.VerificationOperationException;
import io.harness.exception.WingsException;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import software.wings.annotation.EncryptableSetting;
import software.wings.audit.ResourceType;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;
import software.wings.sm.states.APMVerificationState.Method;
import software.wings.utils.Utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@JsonTypeName("APM_VERIFICATION")
@Data
@ToString(exclude = {"headersList", "optionsList"})
@EqualsAndHashCode(callSuper = false)
@Slf4j
public class APMVerificationConfig extends SettingValue implements EncryptableSetting, ExecutionCapabilityDemander {
  @Transient @SchemaIgnore private static final String MASKED_STRING = "*****";
  @Transient @SchemaIgnore private static final String SECRET_REGEX = "\\$\\{secretRef:([^,]*),([^}]*)}";

  @Attributes(title = "Base Url") private String url;

  @NotEmpty @Attributes(title = "Validation Url", required = true) private String validationUrl;

  private String validationBody;

  private Method validationMethod;

  @SchemaIgnore private boolean logVerification;

  @SchemaIgnore @NotEmpty private String accountId;

  private List<KeyValues> headersList;
  private List<KeyValues> optionsList;
  private List<KeyValues> additionalEncryptedFields;
  @SchemaIgnore private Map<String, String> secretIdsToFieldNameMap = new HashMap<>();

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
      SecretManager secretManager, EncryptionService encryptionService, boolean enabledConnectorsRefSecrets) {
    try {
      Map<String, String> headers = new HashMap<>();
      if (!isEmpty(headersList)) {
        for (KeyValues keyValue : headersList) {
          if (enabledConnectorsRefSecrets && keyValue.encrypted) {
            final Optional<EncryptedDataDetail> encryptedDataDetail =
                secretManager.encryptedDataDetails(accountId, keyValue.key, keyValue.value);
            if (!encryptedDataDetail.isPresent()) {
              throw new IllegalStateException("could not find record " + keyValue.value + " for " + keyValue.key);
            }
            headers.put(keyValue.getKey(), new String(encryptionService.getDecryptedValue(encryptedDataDetail.get())));
          } else if (keyValue.encrypted && MASKED_STRING.equals(keyValue.value)) {
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
          if (enabledConnectorsRefSecrets && keyValue.encrypted) {
            final Optional<EncryptedDataDetail> encryptedDataDetail =
                secretManager.encryptedDataDetails(accountId, keyValue.key, keyValue.value);
            if (!encryptedDataDetail.isPresent()) {
              throw new IllegalStateException("could not find record " + keyValue.value + " for " + keyValue.key);
            }
            options.put(keyValue.getKey(), new String(encryptionService.getDecryptedValue(encryptedDataDetail.get())));
          } else if (keyValue.encrypted && MASKED_STRING.equals(keyValue.value)) {
            options.put(keyValue.getKey(),
                new String(encryptionService.getDecryptedValue(
                    secretManager.encryptedDataDetails(accountId, keyValue.key, keyValue.encryptedValue).get())));
          } else {
            options.put(keyValue.getKey(), keyValue.getValue());
          }
        }
      }

      List<KeyValues> bodySecrets = getSecretNameIdKeyValueList(validationBody);
      List<KeyValues> urlSecrets = getSecretNameIdKeyValueList(validationUrl);

      return APMValidateCollectorConfig.builder()
          .baseUrl(url)
          .url(resolveSecretNameInUrlOrBody(validationUrl))
          .body(resolveSecretNameInUrlOrBody(validationBody))
          .collectionMethod(validationMethod)
          .headers(headers)
          .options(options)
          .encryptedDataDetails(encryptedDataDetails(secretManager, Arrays.asList(bodySecrets, urlSecrets)))
          .build();
    } catch (Exception ex) {
      throw new VerificationOperationException(ErrorCode.APM_CONFIGURATION_ERROR, "Unable to validate connector ", ex);
    }
  }

  @Override
  public String fetchResourceCategory() {
    return ResourceType.VERIFICATION_PROVIDER.name();
  }

  @Override
  public List<ExecutionCapability> fetchRequiredExecutionCapabilities() {
    return Arrays.asList(HttpConnectionExecutionCapabilityGenerator.buildHttpConnectionExecutionCapability(
        Utils.appendPathToBaseUrl(getUrl(), getValidationUrl()), QUERY));
  }

  @Override
  public List<String> fetchRelevantEncryptedSecrets() {
    List<String> rv = new ArrayList<>();
    if (isNotEmpty(headersList)) {
      headersList.stream()
          .filter(keyValues -> keyValues.encrypted)
          .forEach(keyValues -> rv.add(keyValues.encryptedValue));
    }

    if (isNotEmpty(optionsList)) {
      optionsList.stream()
          .filter(keyValues -> keyValues.encrypted)
          .forEach(keyValues -> rv.add(keyValues.encryptedValue));
    }

    List<KeyValues> bodySecrets = getSecretNameIdKeyValueList(validationBody);
    bodySecrets.forEach(keyValues -> rv.add(keyValues.encryptedValue));
    List<KeyValues> urlSecrets = getSecretNameIdKeyValueList(validationUrl);
    urlSecrets.forEach(keyValues -> rv.add(keyValues.encryptedValue));
    return rv;
  }

  @Data
  @Builder
  public static class KeyValues {
    private String key;
    private String value;
    private boolean encrypted;
    private String encryptedValue;
  }

  public List<EncryptedDataDetail> encryptedDataDetails(
      SecretManager secretManager, List<List<KeyValues>> keyValueList) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();
    if (isNotEmpty(keyValueList)) {
      keyValueList.forEach(keyValue -> {
        if (isNotEmpty(keyValue)) {
          encryptedDataDetails.addAll(
              keyValue.stream()
                  .filter(entry -> entry.encrypted)
                  .map(entry
                      -> secretManager.encryptedDataDetails(accountId, entry.key, entry.encryptedValue)
                             .<WingsException>orElseThrow(
                                 ()
                                     -> new VerificationOperationException(
                                         ErrorCode.APM_CONFIGURATION_ERROR, "Unable to decrypt field " + entry.key)))
                  .collect(Collectors.toList()));
        }
      });
    }
    return encryptedDataDetails.size() > 0 ? encryptedDataDetails : null;
  }
  public List<EncryptedDataDetail> encryptedDataDetails(SecretManager secretManager) {
    List<EncryptedDataDetail> encryptedDataDetails = new ArrayList<>();

    if (headersList != null) {
      encryptedDataDetails.addAll(
          headersList.stream()
              .filter(entry -> entry.encrypted)
              .map(entry
                  -> secretManager.encryptedDataDetails(accountId, entry.key, entry.encryptedValue)
                         .<WingsException>orElseThrow(
                             ()
                                 -> new VerificationOperationException(
                                     ErrorCode.APM_CONFIGURATION_ERROR, "Unable to decrypt field " + entry.key)))
              .collect(Collectors.toList()));
    }
    if (optionsList != null) {
      encryptedDataDetails.addAll(
          optionsList.stream()
              .filter(entry -> entry.encrypted)
              .map(entry
                  -> secretManager.encryptedDataDetails(accountId, entry.key, entry.encryptedValue)
                         .<WingsException>orElseThrow(
                             ()
                                 -> new VerificationOperationException(
                                     ErrorCode.APM_CONFIGURATION_ERROR, "Unable to decrypt field " + entry.key)))
              .collect(Collectors.toList()));
    }
    return encryptedDataDetails.size() > 0 ? encryptedDataDetails : null;
  }

  public void encryptFields(SecretManager secretManager, boolean enabledConnectorsRefSecrets) {
    secretIdsToFieldNameMap.clear();
    if (headersList != null) {
      headersList.stream()
          .filter(header -> header.encrypted)
          .filter(header -> !header.value.equals(MASKED_STRING))
          .forEach(header -> {
            header.encryptedValue =
                enabledConnectorsRefSecrets ? header.value : secretManager.encrypt(accountId, header.value, null);
            header.value = enabledConnectorsRefSecrets ? header.value : MASKED_STRING;
            secretIdsToFieldNameMap.put(header.value, "header." + header.key);
          });
    }

    if (optionsList != null) {
      optionsList.stream()
          .filter(option -> option.encrypted)
          .filter(option -> !option.value.equals(MASKED_STRING))
          .forEach(option -> {
            option.encryptedValue =
                enabledConnectorsRefSecrets ? option.value : secretManager.encrypt(accountId, option.value, null);
            option.value = enabledConnectorsRefSecrets ? option.value : MASKED_STRING;
            secretIdsToFieldNameMap.put(option.value, "option." + option.key);
          });
    }

    List<KeyValues> bodySecrets = getSecretNameIdKeyValueList(validationBody);
    bodySecrets.forEach(keyValues -> secretIdsToFieldNameMap.put(keyValues.encryptedValue, keyValues.key));
    List<KeyValues> urlSecrets = getSecretNameIdKeyValueList(validationUrl);
    urlSecrets.forEach(keyValues -> secretIdsToFieldNameMap.put(keyValues.encryptedValue, keyValues.key));
  }

  public String resolveSecretNameInUrlOrBody(String url) {
    if (isEmpty(url)) {
      return url;
    }
    Pattern batchPattern = Pattern.compile(SECRET_REGEX);
    Matcher matcher = batchPattern.matcher(url);
    while (matcher.find()) {
      String fullMatch = matcher.group();
      String name = matcher.group(1);
      url = url.replace(fullMatch, "${" + name + "}");
    }
    return url;
  }

  public static List<KeyValues> getSecretNameIdKeyValueList(String url) {
    List<KeyValues> keyValuesList = new ArrayList<>();
    if (!isEmpty(url)) {
      Pattern secretPattern = Pattern.compile(SECRET_REGEX);
      Matcher matcher = secretPattern.matcher(url);
      while (matcher.find()) {
        String name = matcher.group(1);
        String id = matcher.group(2);
        keyValuesList.add(
            KeyValues.builder().encrypted(true).encryptedValue(id).key(name).value(MASKED_STRING).build());
      }
    }
    return keyValuesList;
  }
}
