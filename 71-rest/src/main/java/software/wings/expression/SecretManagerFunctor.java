package software.wings.expression;

import static io.harness.exception.WingsException.USER;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionFunctor;
import lombok.Builder;
import lombok.Builder.Default;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Builder
public class SecretManagerFunctor implements ExpressionFunctor {
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private String accountId;
  @Default private Map<String, String> evaluatedSecrets = new HashMap<>();

  public Object obtain(String secretName) {
    if (evaluatedSecrets.containsKey(secretName)) {
      return evaluatedSecrets.get(secretName);
    }
    EncryptedData encryptedData = secretManager.getSecretByName(accountId, secretName, false);
    if (encryptedData == null) {
      throw new InvalidRequestException("No encrypted record found with secretName + [" + secretName + "]", USER);
    }
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName(secretName)
                                          .build();
    List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(serviceVariable, null, null);
    managerDecryptionService.decrypt(serviceVariable, encryptionDetails);
    String value = new String(serviceVariable.getValue());
    evaluatedSecrets.put(secretName, value);
    return value;
  }
}