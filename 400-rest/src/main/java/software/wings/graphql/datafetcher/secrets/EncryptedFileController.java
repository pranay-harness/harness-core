/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.graphql.datafetcher.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import static org.apache.commons.lang3.StringUtils.isBlank;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.beans.EncryptedData;
import io.harness.beans.SecretFile;
import io.harness.exception.InvalidRequestException;

import software.wings.graphql.schema.mutation.secrets.input.QLCreateSecretInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedFile;
import software.wings.graphql.schema.type.secrets.QLEncryptedFileInput;
import software.wings.graphql.schema.type.secrets.QLEncryptedFileUpdate;
import software.wings.graphql.schema.type.secrets.QLSecretType;
import software.wings.graphql.schema.type.secrets.QLUsageScope;
import software.wings.security.UsageRestrictions;
import software.wings.service.intfc.security.SecretManager;

import com.google.inject.Inject;
import javax.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class EncryptedFileController {
  @Inject SecretManager secretManager;
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

  public String createEncryptedFile(QLCreateSecretInput input, String accountId) {
    QLEncryptedFileInput fileInput = input.getEncryptedFile();
    if (fileInput == null) {
      throw new InvalidRequestException("No encrypted file input provided in the request");
    }

    String secretMangerId = fileInput.getSecretManagerId();
    String secretName = fileInput.getName();
    if (isBlank(secretName)) {
      throw new InvalidRequestException("The name of the secret can not be blank");
    }
    SecretFile secretFile =
        SecretFile.builder()
            .fileContent(fileInput.getFileContent())
            .name(fileInput.getName())
            .kmsId(secretMangerId)
            .hideFromListing(false)
            .usageRestrictions(usageScopeController.populateUsageRestrictions(fileInput.getUsageScope(), accountId))
            .scopedToAccount(fileInput.isScopedToAccount())
            .inheritScopesFromSM(fileInput.isInheritScopesFromSM())
            .build();

    return secretManager.saveSecretFile(accountId, secretFile);
  }

  public void updateEncryptedFile(QLEncryptedFileUpdate encryptedFileUpdate, String encryptedFileId, String accountId) {
    if (encryptedFileUpdate == null) {
      throw new InvalidRequestException(
          "No encrypted file input provided with the request with secretType ENCRYPTED_FILE");
    }

    EncryptedData existingEncryptedData = secretManager.getSecretById(accountId, encryptedFileId);
    if (existingEncryptedData == null) {
      throw new InvalidRequestException(String.format("No encrypted file exists with the id %s", encryptedFileId));
    }
    String name = existingEncryptedData.getName();
    if (encryptedFileUpdate.getName().isPresent()) {
      name = encryptedFileUpdate.getName().getValue().map(StringUtils::strip).orElse(null);
      if (isBlank(name)) {
        throw new InvalidRequestException("Cannot set the value of encrypted file name as blank");
      }
    }

    byte[] value = null;

    // Updating the value
    if (encryptedFileUpdate.getFileContent().isPresent()
        && encryptedFileUpdate.getFileContent().getValue().isPresent()) {
      value = encryptedFileUpdate.getFileContent().getValue().get();
    }

    // Updating the usage Restrictions
    UsageRestrictions usageRestrictions = existingEncryptedData.getUsageRestrictions();
    if (encryptedFileUpdate.getUsageScope().isPresent()) {
      QLUsageScope usageScopeUpdate = encryptedFileUpdate.getUsageScope().getValue().orElse(null);
      usageRestrictions = usageScopeController.populateUsageRestrictions(usageScopeUpdate, accountId);
    }

    boolean scopedToAccount = existingEncryptedData.isScopedToAccount();
    if (encryptedFileUpdate.getScopedToAccount() != null && encryptedFileUpdate.getScopedToAccount().isPresent()) {
      scopedToAccount =
          encryptedFileUpdate.getScopedToAccount().getValue().orElse(existingEncryptedData.isScopedToAccount());
    }

    boolean inheritScopesFromSM = existingEncryptedData.isInheritScopesFromSM();
    if (encryptedFileUpdate.getInheritScopesFromSM() != null
        && encryptedFileUpdate.getInheritScopesFromSM().isPresent()) {
      inheritScopesFromSM =
          encryptedFileUpdate.getInheritScopesFromSM().getValue().orElse(existingEncryptedData.isScopedToAccount());
    }

    SecretFile secretFile = SecretFile.builder()
                                .fileContent(value)
                                .name(name)
                                .hideFromListing(false)
                                .usageRestrictions(usageRestrictions)
                                .scopedToAccount(scopedToAccount)
                                .inheritScopesFromSM(inheritScopesFromSM)
                                .build();

    secretManager.updateSecretFile(accountId, encryptedFileId, secretFile);
  }
}
