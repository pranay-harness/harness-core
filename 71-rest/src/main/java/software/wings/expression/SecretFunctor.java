package software.wings.expression;

import static software.wings.beans.Base.GLOBAL_APP_ID;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.expression.LateBindingMap;
import lombok.Builder;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

@Builder
public class SecretFunctor extends LateBindingMap {
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private String accountId;
  private String appId;
  private String envId;

  public Object getValue(String secretName) {
    EncryptedData encryptedData;
    if (appId == null || GLOBAL_APP_ID.equals(appId)) {
      encryptedData = secretManager.getSecretMappedToAccountByName(accountId, secretName);
    } else {
      encryptedData = secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretName);
    }
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName(secretName)
                                          .build();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(serviceVariable, null, null);
    managerDecryptionService.decrypt(serviceVariable, encryptionDetails);
    return new String(serviceVariable.getValue());
  }

  @Override
  public Object get(Object key) {
    return getValue((String) key);
  }
}
