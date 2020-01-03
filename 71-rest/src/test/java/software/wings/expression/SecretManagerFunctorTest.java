package software.wings.expression;

import static io.harness.rule.OwnerRule.SRINIVAS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import io.harness.category.element.UnitTests;
import io.harness.data.algorithm.HashGenerator;
import io.harness.data.structure.UUIDGenerator;
import io.harness.exception.FunctorException;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.security.ManagerDecryptionService;
import software.wings.service.intfc.security.SecretManager;

import java.util.Arrays;
import java.util.List;

public class SecretManagerFunctorTest extends WingsBaseTest {
  @Mock private ManagerDecryptionService managerDecryptionService;
  @Mock private SecretManager secretManager;
  private static final String ACCOUNT_ID = "ACCOUNT_ID";
  private static final String APP_ID = "APP_ID";
  private static final String WORKFLOW_EXECUTION_ID = "WORKFLOW_EXECUTION_ID";

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDecryptLocalEncryptedServiceVariables() {
    final String secretName = "MySecretName";

    final int token = HashGenerator.generateIntegerHash();

    SecretManagerFunctor secretManagerFunctor = buildFunctor(token);
    assertFunctor(secretManagerFunctor);

    final EncryptedData encryptedData =
        EncryptedData.builder().encryptionType(EncryptionType.LOCAL).accountId(ACCOUNT_ID).build();
    encryptedData.setUuid(UUIDGenerator.generateUuid());

    when(secretManager.getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName)).thenReturn(encryptedData);

    ServiceVariable serviceVariable = buildServiceVariable(secretName, encryptedData);

    List<EncryptedDataDetail> localEncryptedDetails = Arrays.asList(
        EncryptedDataDetail.builder().encryptedData(SecretManager.buildRecordData(encryptedData)).build());

    when(secretManager.getEncryptionDetails(serviceVariable, APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(localEncryptedDetails);

    Mockito
        .doAnswer((Answer<Void>) invocation -> {
          Object[] args = invocation.getArguments();
          ServiceVariable serviceVariable1 = (ServiceVariable) args[0];
          serviceVariable1.setValue("DecryptedValue".toCharArray());
          return null;
        })
        .when(managerDecryptionService)
        .decrypt(serviceVariable, localEncryptedDetails);

    Object decryptedValue = secretManagerFunctor.obtain(secretName, token);
    assertDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager).getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName);

    // Call Second time, it should get it from cache
    decryptedValue = secretManagerFunctor.obtain(secretName, token);
    assertDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager, times(1)).getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName);
    verify(managerDecryptionService, times(1)).decrypt(any(ServiceVariable.class), anyList());
  }

  private void assertDecryptedValue(
      String secretName, SecretManagerFunctor secretManagerFunctor, Object decryptedValue) {
    assertThat(decryptedValue).isNotNull().isEqualTo("DecryptedValue");
    assertThat(secretManagerFunctor.getEvaluatedSecrets()).isNotEmpty().containsKey(secretName);
    assertThat(secretManagerFunctor.getEvaluatedSecrets()).isNotEmpty().containsValues("DecryptedValue");
  }

  private ServiceVariable buildServiceVariable(String secretName, EncryptedData encryptedData) {
    return ServiceVariable.builder()
        .accountId(ACCOUNT_ID)
        .type(ENCRYPTED_TEXT)
        .encryptedValue(encryptedData.getUuid())
        .secretTextName(secretName)
        .build();
  }

  private SecretManagerFunctor buildFunctor(int token) {
    return SecretManagerFunctor.builder()
        .secretManager(secretManager)
        .managerDecryptionService(managerDecryptionService)
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .envId(ENV_ID)
        .workflowExecutionId(WORKFLOW_EXECUTION_ID)
        .expressionFunctorToken(token)
        .build();
  }

  @Test
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldDecryptKMSEncryptedServiceVariables() {
    final String secretName = "MySecretName";
    final int token = HashGenerator.generateIntegerHash();
    SecretManagerFunctor secretManagerFunctor = buildFunctor(token);

    assertFunctor(secretManagerFunctor);

    final EncryptedData encryptedData = EncryptedData.builder().accountId(ACCOUNT_ID).build();
    encryptedData.setUuid(UUIDGenerator.generateUuid());

    when(secretManager.getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName)).thenReturn(encryptedData);

    ServiceVariable serviceVariable = buildServiceVariable(secretName, encryptedData);
    final KmsConfig kmsConfig = KmsConfig.builder().build();
    kmsConfig.setUuid(UUIDGenerator.generateUuid());
    List<EncryptedDataDetail> nonLocalEncryptedVariables =
        Arrays.asList(EncryptedDataDetail.builder()
                          .encryptedData(SecretManager.buildRecordData(encryptedData))
                          .encryptionConfig(kmsConfig)
                          .build());

    when(secretManager.getEncryptionDetails(serviceVariable, APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(nonLocalEncryptedVariables);

    String decryptedValue = (String) secretManagerFunctor.obtain(secretName, token);

    assertDelegateDecryptedValue(secretName, secretManagerFunctor, decryptedValue);

    verify(secretManager).getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName);

    decryptedValue = (String) secretManagerFunctor.obtain(secretName, token);
    assertDelegateDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager, times(1)).getSecretMappedToAppByName(ACCOUNT_ID, APP_ID, ENV_ID, secretName);
  }

  @Test(expected = FunctorException.class)
  @Owner(developers = SRINIVAS)
  @Category(UnitTests.class)
  public void shouldRejectInternalFunctor() {
    final String secretName = "MySecretName";
    final int token = HashGenerator.generateIntegerHash();

    SecretManagerFunctor secretManagerFunctor = buildFunctor(token);
    assertFunctor(secretManagerFunctor);

    secretManagerFunctor.obtain(secretName, HashGenerator.generateIntegerHash());
  }

  private void assertDelegateDecryptedValue(
      String secretName, SecretManagerFunctor secretManagerFunctor, String decryptedValue) {
    assertThat(decryptedValue).isNotNull().startsWith("${secretDelegate.obtain(");
    assertThat(decryptedValue).isNotNull().contains(String.valueOf(secretManagerFunctor.getExpressionFunctorToken()));
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets()).isNotEmpty().containsKey(secretName);
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets().get(secretName)).isNotEmpty();
    assertThat(secretManagerFunctor.getEncryptionConfigs()).isNotEmpty();
  }

  private void assertFunctor(SecretManagerFunctor secretManagerFunctor) {
    assertThat(secretManagerFunctor.getEvaluatedSecrets()).isNotNull();
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets()).isNotNull();
    assertThat(secretManagerFunctor.getEncryptionConfigs()).isNotNull();
    assertThat(secretManagerFunctor.getSecretDetails()).isNotNull();
  }
}
