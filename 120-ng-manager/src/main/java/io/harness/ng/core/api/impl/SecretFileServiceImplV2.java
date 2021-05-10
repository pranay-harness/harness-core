package io.harness.ng.core.api.impl;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.core.api.SecretModifyService;
import io.harness.ng.core.dto.secrets.SecretDTOV2;
import io.harness.secretmanagerclient.dto.EncryptedDataDTO;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.ng.core.SecretManagementModule.SECRET_FILE_SERVICE;

@OwnedBy(PL)
@Singleton
@AllArgsConstructor(onConstructor = @__({ @Inject}))
@Slf4j
public class SecretFileServiceImplV2 implements SecretModifyService {
    @Inject @Named(SECRET_FILE_SERVICE) SecretModifyService secretModifyService;
    @Inject @Named("ngSecretMigrationCompleted") boolean ngSecretMigrationCompleted;

    @Override
    public EncryptedDataDTO create(String accountIdentifier, SecretDTOV2 dto) {
        if (!ngSecretMigrationCompleted) {
            secretModifyService.create(accountIdentifier, dto);
        }
        return null;
    }

    @Override
    public void validateUpdateRequest(SecretDTOV2 existingSecret, SecretDTOV2 dto) {
        secretModifyService.validateUpdateRequest(existingSecret, dto);
    }

    @Override
    public boolean update(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
        if (!ngSecretMigrationCompleted) {
            secretModifyService.update(accountIdentifier, existingSecret, dto);
        }
        return false;
    }

    @Override
    public boolean updateViaYaml(String accountIdentifier, SecretDTOV2 existingSecret, SecretDTOV2 dto) {
        if (!ngSecretMigrationCompleted) {
            secretModifyService.updateViaYaml(accountIdentifier, existingSecret, dto);
        }
        return false;
    }
}
