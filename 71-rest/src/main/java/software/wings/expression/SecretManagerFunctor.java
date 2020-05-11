package software.wings.expression;

import static io.harness.annotations.dev.HarnessTeam.CDC;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.exception.WingsException.USER;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.SecretDetail;
import io.harness.exception.FunctorException;
import io.harness.exception.InvalidRequestException;
import io.harness.expression.ExpressionFunctor;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Value;
import software.wings.beans.FeatureName;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDC)
@Value
@Builder
public class SecretManagerFunctor implements ExpressionFunctor, SecretManagerFunctorInterface {
  public enum Mode {
    APPLY,
    DRY_RUN,
  }
  private Mode mode;
  private FeatureFlagService featureFlagService;
  private ManagerDecryptionService managerDecryptionService;
  private SecretManager secretManager;
  private String accountId;
  private String appId;
  private String envId;
  private String workflowExecutionId;
  private int expressionFunctorToken;

  @Default private Map<String, String> evaluatedSecrets = new HashMap<>();
  @Default private Map<String, String> evaluatedDelegateSecrets = new HashMap<>();
  @Default private Map<String, EncryptionConfig> encryptionConfigs = new HashMap<>();
  @Default private Map<String, SecretDetail> secretDetails = new HashMap<>();

  @Override
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

  private Object returnValue(String secretName, Object value) {
    if (mode == Mode.DRY_RUN) {
      return "${secretManager.obtain(\"" + secretName + "\", " + expressionFunctorToken + ")}";
    }
    return value;
  }

  private Object obtainInternal(String secretName) {
    if (evaluatedSecrets.containsKey(secretName)) {
      return returnValue(secretName, evaluatedSecrets.get(secretName));
    }
    if (evaluatedDelegateSecrets.containsKey(secretName)) {
      return returnValue(secretName, evaluatedDelegateSecrets.get(secretName));
    }

    EncryptedData encryptedData = secretManager.getSecretMappedToAppByName(accountId, appId, envId, secretName);
    if (encryptedData == null) {
      throw new InvalidRequestException("No secret found with name + [" + secretName + "]", USER);
    }
    ServiceVariable serviceVariable = ServiceVariable.builder()
                                          .accountId(accountId)
                                          .type(ENCRYPTED_TEXT)
                                          .encryptedValue(encryptedData.getUuid())
                                          .secretTextName(secretName)
                                          .build();

    List<EncryptedDataDetail> encryptedDataDetails =
        secretManager.getEncryptionDetails(serviceVariable, appId, workflowExecutionId);

    boolean enabled = featureFlagService.isEnabled(FeatureName.THREE_PHASE_SECRET_DECRYPTION, accountId);

    List<EncryptedDataDetail> localEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail
                -> !enabled || encryptedDataDetail.getEncryptedData().getEncryptionType() == EncryptionType.LOCAL)
            .collect(Collectors.toList());

    if (isNotEmpty(localEncryptedDetails)) {
      managerDecryptionService.decrypt(serviceVariable, localEncryptedDetails);
      String value = new String(serviceVariable.getValue());
      evaluatedSecrets.put(secretName, value);
      return returnValue(secretName, value);
    }

    List<EncryptedDataDetail> nonLocalEncryptedDetails =
        encryptedDataDetails.stream()
            .filter(encryptedDataDetail
                -> encryptedDataDetail.getEncryptedData().getEncryptionType() != EncryptionType.LOCAL)
            .collect(Collectors.toList());

    if (nonLocalEncryptedDetails.size() != 1) {
      throw new InvalidRequestException("More than one encrypted records associated with + [" + secretName + "]", USER);
    }

    EncryptedDataDetail encryptedDataDetail = nonLocalEncryptedDetails.get(0);

    String encryptionConfigUuid = encryptedDataDetail.getEncryptionConfig().getUuid();
    encryptionConfigs.put(encryptionConfigUuid, encryptedDataDetail.getEncryptionConfig());

    SecretDetail secretDetail =
        SecretDetail.builder().configUuid(encryptionConfigUuid).encryptedRecord(encryptedData).build();

    String secretDetailsUuid = generateUuid();

    secretDetails.put(secretDetailsUuid, secretDetail);
    evaluatedDelegateSecrets.put(
        secretName, "${secretDelegate.obtain(\"" + secretDetailsUuid + "\", " + expressionFunctorToken + ")}");
    return returnValue(secretName, evaluatedDelegateSecrets.get(secretName));
  }
}
