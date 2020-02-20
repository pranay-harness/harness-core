package software.wings.graphql.datafetcher.secrets;

import static io.harness.expression.SecretString.SECRET_MASK;
import static org.apache.commons.lang3.StringUtils.isBlank;

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

    String secretName = encryptedText.getName();
    if (isBlank(secretName)) {
      throw new InvalidRequestException("The name of the secret can not be blank");
    }

    String secretValue = encryptedText.getValue();
    if (isBlank(secretValue)) {
      throw new InvalidRequestException("The value of the secret cannot be blank");
    }

    return secretManager.saveSecret(accountId, encryptedText.getSecretManagerId(), encryptedText.getName(),
        encryptedText.getValue(), null,
        usageScopeController.populateUsageRestrictions(encryptedText.getUsageScope(), accountId));
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

      EncryptedData encryptedData = secretManager.getSecretByName(accountId, name);
      if (encryptedData != null && !encryptedData.getUuid().equals(encryptedTextId)) {
        throw new InvalidRequestException(String.format("A secret already exists with the name %s", name));
      }
    }
    String value = SECRET_MASK;
    if (encryptedTextUpdate.getValue().isPresent()) {
      value = encryptedTextUpdate.getValue().getValue().orElse(null);
      if (isBlank(value)) {
        throw new InvalidRequestException("Cannot set the value of encrypted text value as blank");
      }
      EncryptedData existingEncryptedText = secretManager.getSecretById(accountId, encryptedTextId);
      if (existingEncryptedText == null) {
        throw new InvalidRequestException(String.format("No encrypted text exists with the id %s", encryptedTextId));
      }
    }

    UsageRestrictions usageRestrictions = null;
    if (encryptedTextUpdate.getUsageScope().isPresent()) {
      QLUsageScope usageScopeUpdate = encryptedTextUpdate.getUsageScope().getValue().orElse(null);
      usageRestrictions = usageScopeController.populateUsageRestrictions(usageScopeUpdate, accountId);
    }
    secretManager.updateSecret(accountId, encryptedTextId, name, value, null, usageRestrictions);
  }
}
