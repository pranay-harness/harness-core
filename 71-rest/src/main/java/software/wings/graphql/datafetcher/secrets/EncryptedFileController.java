package software.wings.graphql.datafetcher.secrets;

import com.google.inject.Inject;

import software.wings.graphql.schema.type.secrets.QLEncryptedFile;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.security.encryption.EncryptedData;

import javax.validation.constraints.NotNull;

public class EncryptedFileController {
  @Inject UsageScopeController usageScopeController;

  public QLEncryptedFile populateEncryptedFile(@NotNull EncryptedData encryptedFile) {
    return QLEncryptedFile.builder()
        .id(encryptedFile.getUuid())
        .secretType(QLSecretType.ENCRYPTED_FILE)
        .secretManagerId(encryptedFile.getKmsId())
        .name(encryptedFile.getName())
        .usageScope(usageScopeController.populateUsageScope(encryptedFile.getUsageRestrictions()))
        .build();
  }
}
