package software.wings.service.intfc.security;

import io.harness.beans.DecryptableEntity;
import io.harness.beans.EncryptedData;
import io.harness.beans.PageResponse;
import io.harness.beans.SecretManagerConfig;
import io.harness.ng.core.NGAccess;
import io.harness.secretmanagerclient.dto.SecretTextDTO;
import io.harness.secretmanagerclient.dto.SecretTextUpdateDTO;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.settings.SettingVariableTypes;

import java.util.List;
import java.util.Optional;
import javax.validation.constraints.NotNull;

public interface NGSecretService {
  EncryptedData createSecretText(SecretTextDTO dto);

  PageResponse<EncryptedData> listSecrets(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SettingVariableTypes settingVariableTypes, String page, String size);

  Optional<EncryptedData> get(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String identifier);

  boolean updateSecretText(String account, String org, String project, String identifier, SecretTextUpdateDTO dto);

  boolean deleteSecretText(
      @NotNull String accountIdentifier, String orgIdentifier, String projectIdentifier, @NotNull String identifier);

  List<EncryptedDataDetail> getEncryptionDetails(NGAccess ngAccess, DecryptableEntity object);

  List<EncryptedData> searchSecrets(String accountIdentifier, String orgIdentifier, String projectIdentifier,
      SettingVariableTypes type, String searchTerm);

  void deleteSecretInSecretManager(
      String accountIdentifier, EncryptedData encryptedData, SecretManagerConfig secretManagerConfig);
}
