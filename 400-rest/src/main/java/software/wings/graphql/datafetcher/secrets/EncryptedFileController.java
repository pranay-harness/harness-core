package software.wings.graphql.datafetcher.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;

import software.wings.graphql.schema.type.secrets.QLEncryptedFile;
import software.wings.graphql.schema.type.secrets.QLSecretType;

import com.google.inject.Inject;
import javax.validation.constraints.NotNull;

@TargetModule(Module._380_CG_GRAPHQL)
public class EncryptedFileController {
  @Inject UsageScopeController usageScopeController;

  public QLEncryptedFile populateEncryptedFile(@NotNull EncryptedData encryptedFile) {
    return QLEncryptedFile.builder()
        .id(encryptedFile.getUuid())
        .secretType(QLSecretType.ENCRYPTED_FILE)
        .secretManagerId(encryptedFile.getKmsId())
        .name(encryptedFile.getName())
        .usageScope(usageScopeController.populateUsageScope(encryptedFile.getUsageRestrictions()))
        .scopedToAccount(encryptedFile.isScopedToAccount())
        .inheritScopesFromSM(encryptedFile.isInheritScopesFromSM())
        .build();
  }
}
