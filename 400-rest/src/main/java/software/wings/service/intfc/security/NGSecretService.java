package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;

import java.util.Optional;
import javax.validation.constraints.NotNull;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_CORE)
public interface NGSecretService {
  boolean save(EncryptedDataMigrationDTO encryptedData);

  boolean update(EncryptedDataMigrationDTO encryptedData);

  Optional<EncryptedDataMigrationDTO> getEncryptedDataMigrationDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean delete(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier);
}
