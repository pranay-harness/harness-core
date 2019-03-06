package software.wings.delegate.service;

import static org.joor.Reflect.on;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.SecretDetail;
import io.harness.security.encryption.DelegateDecryptionService;
import io.harness.security.encryption.EncryptionConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import retrofit2.Call;
import software.wings.beans.DelegatePackage;
import software.wings.beans.DelegateTask;
import software.wings.beans.KmsConfig;
import software.wings.managerclient.ManagerClient;
import software.wings.security.encryption.EncryptedData;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;

public class DelegateServiceImplTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock ManagerClient managerClient;
  @Mock Call<DelegatePackage> delegatePackageCall;
  @Mock private Injector injector;
  @Mock private ExecutorService asyncExecutorService;
  @Mock private DelegateDecryptionService delegateDecryptionService;

  @InjectMocks DelegateServiceImpl delegateService = new DelegateServiceImpl();

  @Before
  public void setUp() {
    on(delegateService).set("managerClient", managerClient);
    on(delegateService).set("injector", injector);
    on(delegateService).set("asyncExecutorService", asyncExecutorService);
    on(delegateService).set("delegateDecryptionService", delegateDecryptionService);
    when(managerClient.acquireTask(Matchers.anyString(), Matchers.anyString(), Matchers.anyString()))
        .thenReturn(delegatePackageCall);
  }

  @Test
  public void shouldNotApplyFunctorIfNoSecrets() throws Exception {
    final String delegateTaskId = UUIDGenerator.generateUuid();

    final DelegatePackage delegatePackage =
        DelegatePackage.builder()
            .delegateTask(DelegateTask.builder().async(true).taskType("HTTP").uuid(delegateTaskId).build())
            .build();

    delegateService.applyDelegateSecretFunctor(delegatePackage);
    verify(delegateDecryptionService, times(0)).decrypt(anyMap());
  }

  @Test
  public void shouldApplyFunctorForSecrets() throws Exception {
    final String delegateTaskId = UUIDGenerator.generateUuid();

    Map<String, EncryptionConfig> encryptionConfigMap = new HashMap<>();
    KmsConfig kmsConfig = KmsConfig.builder().build();
    kmsConfig.setUuid("KMS_CONFIG_UUID");
    encryptionConfigMap.put("KMS_CONFIG_UUID", kmsConfig);

    Map<String, SecretDetail> secretDetails = new HashMap<>();
    SecretDetail secretDetail = SecretDetail.builder()
                                    .configUuid("KMS_CONFIG_UUID")
                                    .encryptedRecord(EncryptedData.builder().accountId("ACCOUNT_ID").build())
                                    .build();

    secretDetails.put("SECRET_UUID", secretDetail);

    final DelegatePackage delegatePackage =
        DelegatePackage.builder()
            .delegateTask(DelegateTask.builder().async(true).taskType("HTTP").uuid(delegateTaskId).build())
            .encryptionConfigs(encryptionConfigMap)
            .secretDetails(secretDetails)
            .build();

    delegateService.applyDelegateSecretFunctor(delegatePackage);
    verify(delegateDecryptionService, times(1)).decrypt(anyMap());
  }
}
