package software.wings.beans;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.annotations.Transient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.annotation.Encryptable;
import software.wings.exception.WingsException;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.EncryptionService;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.SettingValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@JsonTypeName("APM_VERIFICATION")
@Data
@EqualsAndHashCode(callSuper = false)
@ToString(exclude = {"headers", "options"})
public class APMVerificationConfig extends SettingValue implements Encryptable {
  @Transient @SchemaIgnore private static final Logger logger = LoggerFactory.getLogger(APMVerificationConfig.class);
  @Transient @SchemaIgnore private static final String MASKED_STRING = "*****";

  @Attributes(title = "Base Url") private String url;

  @NotEmpty @Attributes(title = "Validation Url", required = true) private String validationUrl;

  @SchemaIgnore @NotEmpty private String accountId;

  private List<KeyValues> headersList;
  private List<KeyValues> optionsList;

  /**
   * Instantiates a new config.
   */
  public APMVerificationConfig() {
    super(SettingVariableTypes.APM_VERIFICATION.name());
  }

  public APMVerificationConfig(SettingVariableTypes type) {
    super(type.name());
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
          .headers(headers)
          .options(options)
          .build();
    } catch (Exception ex) {
      throw new WingsException("Unable to validate connector ", ex);
    }
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
            header.encryptedValue = secretManager.encrypt(accountId, header.value);
            header.value = MASKED_STRING;
          });
    }

    if (optionsList != null) {
      optionsList.stream()
          .filter(option -> option.encrypted)
          .filter(option -> !option.value.equals(MASKED_STRING))
          .forEach(option -> {
            option.encryptedValue = secretManager.encrypt(accountId, option.value);
            option.value = MASKED_STRING;
          });
    }
  }
}
