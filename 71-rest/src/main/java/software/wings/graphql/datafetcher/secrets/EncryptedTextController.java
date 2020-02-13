package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedText;
import software.wings.graphql.schema.type.secrets.QLEncryptedTextInput;
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
}
