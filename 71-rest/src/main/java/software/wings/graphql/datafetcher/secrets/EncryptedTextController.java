package software.wings.graphql.datafetcher.secrets;

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
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.SecretManager;

import javax.validation.constraints.NotNull;

@Slf4j
@Singleton
public class EncryptedTextController {
  @Inject SecretManager secretManager;
  public QLEncryptedText populateEncryptedText(@NotNull EncryptedData encryptedText) {
    return QLEncryptedText.builder()
        .id(encryptedText.getUuid())
        .secretType(QLSecretType.ENCRYPTED_TEXT)
        .secretManagerId(encryptedText.getKmsId())
        .name(encryptedText.getName())
        .build();
  }

  public String createEncryptedText(QLCreateSecretInput input, String accountId) {
    QLEncryptedTextInput encryptedText = input.getEncryptedText();
    if (encryptedText == null) {
      throw new InvalidRequestException("No encrypted text input provided in the request");
    }
    return secretManager.saveSecret(
        accountId, encryptedText.getSecretManagerId(), encryptedText.getName(), encryptedText.getValue(), null, null);
  }

  public void updateEncryptedText(QLEncryptedTextUpdate encryptedTextUpdate, String encryptedTextId, String accountId) {
    if (encryptedTextUpdate == null) {
      throw new InvalidRequestException(
          "No encrypted text input provided with the request with secretType ENCRYPTED_TEXT");
    }
    String name = encryptedTextUpdate.getName().getValue().map(StringUtils::strip).orElse(null);
    if (isBlank(name)) {
      throw new InvalidRequestException("Cannot set the value of encrypted text name as blank");
    }

    EncryptedData encryptedData = secretManager.getSecretByName(accountId, name);
    if (encryptedData != null && !encryptedData.getUuid().equals(encryptedTextId)) {
      throw new InvalidRequestException(String.format("A secret already exists with the name %s", name));
    }

    String value = encryptedTextUpdate.getValue().getValue().orElse(null);
    if (isBlank(value)) {
      throw new InvalidRequestException("Cannot set the value of encrypted text value as blank");
    }
    EncryptedData existingEncryptedText = secretManager.getSecretById(accountId, encryptedTextId);
    if (existingEncryptedText == null) {
      throw new InvalidRequestException(String.format("No encrypted text exists with the id %s", encryptedTextId));
    }

    secretManager.updateSecret(accountId, encryptedTextId, name, value, null, null);
  }
}
