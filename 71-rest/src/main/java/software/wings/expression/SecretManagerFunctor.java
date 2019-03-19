package software.wings.expression;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.delegate.beans.SecretDetail;
import io.harness.exception.FunctorException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionFunctor;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Value
@Builder
public class SecretManagerFunctor implements ExpressionFunctor {
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private String accountId;
  private String appId;
  private String workflowExecutionId;
  private int expressionFunctorToken;

  @Default private Map<String, String> evaluatedSecrets = new HashMap<>();
  @Default private Map<String, String> evaluatedDelegateSecrets = new HashMap<>();
  @Default private Map<String, EncryptionConfig> encryptionConfigs = new HashMap<>();
  @Default private Map<String, SecretDetail> secretDetails = new HashMap<>();

  public Object obtain(String secretName, int token) {
    if (token != expressionFunctorToken) {
      throw new FunctorException("Inappropriate usage of internal functor");
    }
    try {
      return obtainInternal(secretName);
    } catch (Exception ex) {
      throw new FunctorException("Error occurred while evaluating the secret [" + secretName + "]", ex);
    }
  }

  private Object obtainInternal(String secretName) {
    if (evaluatedSecrets.containsKey(secretName)) {
      return evaluatedSecrets.get(secretName);
    }
    if (evaluatedDelegateSecrets.containsKey(secretName)) {
      return evaluatedDelegateSecrets.get(secretName);
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

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId);

    final List<EncryptedDataDetail> localEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail -> encryptedDataDetail.getEncryptionType() == EncryptionType.LOCAL)
            .collect(Collectors.toList());

    if (isNotEmpty(localEncryptedDetails)) {
      managerDecryptionService.decrypt(serviceVariable, localEncryptedDetails);
      String value = new String(serviceVariable.getValue());
      evaluatedSecrets.put(secretName, value);
      return value;
    }

    List<EncryptedDataDetail> nonLocalEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail -> encryptedDataDetail.getEncryptionType() != EncryptionType.LOCAL)
            .collect(Collectors.toList());

    if (nonLocalEncryptedDetails.size() != 1) {
      throw new InvalidRequestException("More than one encrypted records associated with + [" + secretName + "]", USER);
    }

    EncryptedDataDetail encryptedDataDetail = nonLocalEncryptedDetails.get(0);

    final String encryptionConfigUuid = encryptedDataDetail.getEncryptionConfig().getUuid();
    encryptionConfigs.put(encryptionConfigUuid, encryptedDataDetail.getEncryptionConfig());

    SecretDetail secretDetail =
        SecretDetail.builder().configUuid(encryptionConfigUuid).encryptedRecord(encryptedData).build();

    String secretDetailsUuid = generateUuid();

    secretDetails.put(secretDetailsUuid, secretDetail);
    evaluatedDelegateSecrets.put(
        secretName, "${secretDelegate.obtain(\"" + secretDetailsUuid + "\", " + expressionFunctorToken + ")}");
    return evaluatedDelegateSecrets.get(secretName);
  }
}