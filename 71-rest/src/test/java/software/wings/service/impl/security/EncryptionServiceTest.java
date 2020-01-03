package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptableSettingWithEncryptionDetails;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import software.wings.beans.KmsConfig;
import software.wings.service.intfc.security.SecretManagementDelegateService;

import java.util.List;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author marklu on 10/14/19
 */
public class EncryptionServiceTest extends CategoryTest {
  @Mock private SecretManagementDelegateService secretManagementDelegateService;
  @Mock private EncryptedDataDetail encryptedDataDetail1;
  @Mock private EncryptedDataDetail encryptedDataDetail2;

  private EncryptionServiceImpl encryptionService;
  private ScheduledThreadPoolExecutor threadPoolExecutor = new ScheduledThreadPoolExecutor(4);

  @Before
  public void setUp() {
    initMocks(this);

    encryptionService = new EncryptionServiceImpl(secretManagementDelegateService, threadPoolExecutor);

    EncryptionConfig encryptionConfig = mock(KmsConfig.class);
    when(encryptionConfig.getEncryptionType()).thenReturn(EncryptionType.KMS);

    when(encryptedDataDetail1.getFieldName()).thenReturn("value");
    when(encryptedDataDetail1.getEncryptionConfig()).thenReturn(encryptionConfig);

    when(encryptedDataDetail2.getFieldName()).thenReturn("value");
    when(encryptedDataDetail2.getEncryptionConfig()).thenReturn(encryptionConfig);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testBatchDecryption() {
    String accountId = UUIDGenerator.generateUuid();
    List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails =
        SecurityTestUtils.getEncryptableSettingWithEncryptionDetailsList(
            accountId, Lists.newArrayList(encryptedDataDetail1, encryptedDataDetail2));

    List<EncryptableSettingWithEncryptionDetails> resultDetailsList =
        encryptionService.decrypt(encryptableSettingWithEncryptionDetails);
    assertThat(resultDetailsList).isNotEmpty();
    assertThat(resultDetailsList.size()).isEqualTo(encryptableSettingWithEncryptionDetails.size());

    for (EncryptableSettingWithEncryptionDetails details : resultDetailsList) {
      assertThat(details.getEncryptableSetting().isDecrypted()).isTrue();
    }
  }
}
