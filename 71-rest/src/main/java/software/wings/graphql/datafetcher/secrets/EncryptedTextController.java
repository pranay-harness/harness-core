package software.wings.graphql.datafetcher.secrets;

import static io.harness.expression.SecretString.SECRET_MASK;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextUpdate;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretText;
import software.wings.service.intfc.security.SecretManager;
import software.wings.settings.UsageRestrictions;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class EncryptedTextController {
  @Inject SecretManager secretManager;
  @Inject UsageScopeController usageScopeController;

  public QLEncryptedText populateEncryptedText(@NotNull EncryptedData encryptedText) {
    return QLEncryptedText.builder()
        .id(encryptedText.getUuid())
        .secretType(QLSecretType.ENCRYPTED_TEXT)
        .secretManagerId(encryptedText.getKmsId())
        .name(encryptedText.getName())
        .usageScope(usageScopeController.populateUsageScope(encryptedText.getUsageRestrictions()))
        .build();
  }

  public String createEncryptedText(QLCreateSecretInput input, String accountId) {
    QLEncryptedTextInput encryptedText = input.getEncryptedText();
    if (encryptedText == null) {
      throw new InvalidRequestException("No encrypted text input provided in the request");
    }

    String secretMangerId = encryptedText.getSecretManagerId();
    String secretName = encryptedText.getName();
    if (isBlank(secretName)) {
      throw new InvalidRequestException("The name of the secret can not be blank");
    }

    String secretValue = encryptedText.getValue();
    String path = encryptedText.getSecretReference();
    if (isNotBlank(secretValue) && isNotBlank(path)) {
      throw new InvalidRequestException("Cannot set both value and secret reference for the encrypted text secret");
    }

    if (isBlank(path) && isBlank(secretValue)) {
      throw new InvalidRequestException("Supply either the secret path or the secret value");
    }

    if (secretValue != null) {
      secretManager.validateThatSecretManagerSupportsText(accountId, secretMangerId);
    }

    SecretText secretText =
        SecretText.builder()
            .name(secretName)
            .kmsId(secretMangerId)
            .value(secretValue)
            .path(path)
            .usageRestrictions(usageScopeController.populateUsageRestrictions(encryptedText.getUsageScope(), accountId))
            .build();
    return secretManager.saveSecret(accountId, secretText);
  }

  public void updateEncryptedText(QLEncryptedTextUpdate encryptedTextUpdate, String encryptedTextId, String accountId) {
    if (encryptedTextUpdate == null) {
      throw new InvalidRequestException(
          "No encrypted text input provided with the request with secretType ENCRYPTED_TEXT");
    }

    EncryptedData exitingEncryptedData = secretManager.getSecretById(accountId, encryptedTextId);
    if (exitingEncryptedData == null) {
      throw new InvalidRequestException(String.format("No encrypted text exists with the id %s", encryptedTextId));
    }
    String name = exitingEncryptedData.getName();
    if (encryptedTextUpdate.getName().isPresent()) {
      name = encryptedTextUpdate.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("Cannot set the value of encrypted text name as blank");
      }
    }

    // Updating the secret value
    if (encryptedTextUpdate.getValue().isPresent() && encryptedTextUpdate.getSecretReference().isPresent()) {
      throw new InvalidRequestException("Cannot update both value and secret reference for the encrypted text secret");
    }

    String secretReference = exitingEncryptedData.getPath();
    // If we do not want to change the value variable, then its value will be SECRET_MASK if value is already set
    String value = secretReference == null ? SECRET_MASK : null;

    // Updating the value
    if (encryptedTextUpdate.getValue().isPresent()) {
      secretManager.validateThatSecretManagerSupportsText(accountId, exitingEncryptedData.getKmsId());
      value = encryptedTextUpdate.getValue().getValue().orElse(null);
      secretReference = null;
      if (isBlank(value)) {
        throw new InvalidRequestException("Cannot set the value of encrypted text value as blank");
      }
    }

    // Updating the path
    if (encryptedTextUpdate.getSecretReference().isPresent()) {
      secretReference = encryptedTextUpdate.getSecretReference().getValue().orElse(null);
      value = null;
      if (isBlank(secretReference)) {
        throw new InvalidRequestException("Cannot set the value of encrypted text reference as blank");
      }
    }

    // Updating the usage Restrictions
    UsageRestrictions usageRestrictions = null;
    if (encryptedTextUpdate.getUsageScope().isPresent()) {
      QLUsageScope usageScopeUpdate = encryptedTextUpdate.getUsageScope().getValue().orElse(null);
      usageRestrictions = usageScopeController.populateUsageRestrictions(usageScopeUpdate, accountId);
    }
    SecretText secretText =
        SecretText.builder().name(name).value(value).path(secretReference).usageRestrictions(usageRestrictions).build();
    secretManager.updateSecret(accountId, encryptedTextId, secretText);
  }
}
