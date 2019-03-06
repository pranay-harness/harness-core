package software.wings.expression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceVariable.Type.ENCRYPTED_TEXT;

import io.harness.data.structure.UUIDGenerator;
import io.harness.security.encryption.EncryptionType;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import software.wings.WingsBaseTest;
import software.wings.beans.KmsConfig;
import software.wings.beans.ServiceVariable;
import software.wings.security.encryption.EncryptedData;
import software.wings.security.encryption.EncryptedDataDetail;
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
  public void shouldDecryptLocalEncryptedServiceVariables() {
    final String secretName = "MySecretName";

    SecretManagerFunctor secretManagerFunctor = buildFunctor();
    assertFunctor(secretManagerFunctor);

    final EncryptedData encryptedData = EncryptedData.builder().accountId(ACCOUNT_ID).build();
    encryptedData.setUuid(UUIDGenerator.generateUuid());

    when(secretManager.getSecretByName(ACCOUNT_ID, secretName, false)).thenReturn(encryptedData);

    ServiceVariable serviceVariable = buildServiceVariable(secretName, encryptedData);

    List<EncryptedDataDetail> localEncryptedDetails = Arrays.asList(
        EncryptedDataDetail.builder().encryptedData(encryptedData).encryptionType(EncryptionType.LOCAL).build());

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

    Object decryptedValue = secretManagerFunctor.obtain(secretName);
    assertDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager).getSecretByName(ACCOUNT_ID, secretName, false);

    // Call Second time, it should get it from cache
    decryptedValue = secretManagerFunctor.obtain(secretName);
    assertDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager, times(1)).getSecretByName(ACCOUNT_ID, secretName, false);
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

  private SecretManagerFunctor buildFunctor() {
    return SecretManagerFunctor.builder()
        .secretManager(secretManager)
        .managerDecryptionService(managerDecryptionService)
        .accountId(ACCOUNT_ID)
        .appId(APP_ID)
        .workflowExecutionId(WORKFLOW_EXECUTION_ID)
        .build();
  }

  @Test
  public void shouldDecryptKMSEncryptedServiceVariables() {
    final String secretName = "MySecretName";

    SecretManagerFunctor secretManagerFunctor = buildFunctor();

    assertFunctor(secretManagerFunctor);

    final EncryptedData encryptedData = EncryptedData.builder().accountId(ACCOUNT_ID).build();
    encryptedData.setUuid(UUIDGenerator.generateUuid());

    when(secretManager.getSecretByName(ACCOUNT_ID, secretName, false)).thenReturn(encryptedData);

    ServiceVariable serviceVariable = buildServiceVariable(secretName, encryptedData);
    final KmsConfig kmsConfig = KmsConfig.builder().build();
    kmsConfig.setUuid(UUIDGenerator.generateUuid());
    List<EncryptedDataDetail> nonLocalEncryptedVariables = Arrays.asList(EncryptedDataDetail.builder()
                                                                             .encryptedData(encryptedData)
                                                                             .encryptionConfig(kmsConfig)
                                                                             .encryptionType(EncryptionType.KMS)
                                                                             .build());

    when(secretManager.getEncryptionDetails(serviceVariable, APP_ID, WORKFLOW_EXECUTION_ID))
        .thenReturn(nonLocalEncryptedVariables);

    String decryptedValue = (String) secretManagerFunctor.obtain(secretName);

    assertDelegateDecryptedValue(secretName, secretManagerFunctor, decryptedValue);

    verify(secretManager).getSecretByName(ACCOUNT_ID, secretName, false);

    decryptedValue = (String) secretManagerFunctor.obtain(secretName);
    assertDelegateDecryptedValue(secretName, secretManagerFunctor, decryptedValue);
    verify(secretManager, times(1)).getSecretByName(ACCOUNT_ID, secretName, false);
  }

  private void assertDelegateDecryptedValue(
      String secretName, SecretManagerFunctor secretManagerFunctor, String decryptedValue) {
    assertThat(decryptedValue).isNotNull().startsWith("${secretDelegate.obtain(");
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets()).isNotEmpty().containsKey(secretName);
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets().get(secretName));
    assertThat(secretManagerFunctor.getEncryptionConfigs()).isNotEmpty();
  }

  private void assertFunctor(SecretManagerFunctor secretManagerFunctor) {
    assertThat(secretManagerFunctor.getEvaluatedSecrets()).isNotNull();
    assertThat(secretManagerFunctor.getEvaluatedDelegateSecrets()).isNotNull();
    assertThat(secretManagerFunctor.getEncryptionConfigs()).isNotNull();
    assertThat(secretManagerFunctor.getSecretDetails()).isNotNull();
  }
}
