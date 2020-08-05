package software.wings.service.intfc.security;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.PageResponse;
import io.harness.ng.core.NGAccess;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.security.encryption.EncryptedData;
import software.wings.settings.SettingVariableTypes;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NGSecretService {
  EncryptedData createSecretText(EncryptedData encryptedData, String secretValue);

  PageResponse<EncryptedData> listSecrets(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SettingVariableTypes settingVariableTypes, String limit, String offset);

  Optional<EncryptedData> getSecretText(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean updateSecretText(EncryptedData encryptedData, String secretValue);

  boolean deleteSecretText(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier);

  List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object);
}
