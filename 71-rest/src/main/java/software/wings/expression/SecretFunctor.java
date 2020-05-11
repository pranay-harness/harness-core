package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.Application.GLOBAL_APP_ID;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.LateBindingMap;
import io.harness.expression.SecretString;
import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.List;

@OwnedBy(CDC)
@Builder
public class SecretFunctor extends LateBindingMap {
  private FeatureFlagService featureFlagService;
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private String accountId;
  private String appId;
  private String envId;
  private boolean adoptDelegateDecryption;
  private int expressionFunctorToken;

  public Object getValue(String secretName) {
    if (adoptDelegateDecryption && featureFlagService != null) {
      if (featureFlagService.isEnabled(FeatureName.TWO_PHASE_SECRET_DECRYPTION, accountId)
          || featureFlagService.isEnabled(FeatureName.THREE_PHASE_SECRET_DECRYPTION, accountId)) {
        return "${secretManager.obtain(\"" + secretName + "\", " + expressionFunctorToken + ")}";
      }
    }

    EncryptedData encryptedData = appId == null || GLOBAL_APP_ID.equals(appId)
        ? secretManager.getSecretMappedToAccountByName(accountId, secretName)
        : secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretName);
    if (encryptedData == null) {
      throw new InvalidRequestException("No secret found with name + [" + secretName + "]", USER);
    }
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName(secretName)
                                          .build();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(serviceVariable, null, null);
    managerDecryptionService.decrypt(serviceVariable, encryptionDetails);
    return SecretString.builder().value(new String(serviceVariable.getValue())).build();
  }

  @Override
  public Object get(Object key) {
    return getValue((String) key);
  }
}
