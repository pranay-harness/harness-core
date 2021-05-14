package software.wings.service.intfc.security;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.secretmanagerclient.dto.EncryptedDataMigrationDTO;

import java.util.Optional;

@OwnedBy(PL)
@TargetModule(HarnessModule._950_NG_CORE)
public interface NGSecretService {
  boolean deleteSecretText(String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  Optional<EncryptedDataMigrationDTO> getEncryptedDataMigrationDTO(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);
}
