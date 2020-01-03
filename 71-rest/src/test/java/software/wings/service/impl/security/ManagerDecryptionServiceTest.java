package software.wings.service.impl.security;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
import software.wings.beans.SyncTaskContext;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.service.intfc.security.EncryptionService;

import java.util.List;

/**
 * @author marklu on 10/18/19
 */
public class ManagerDecryptionServiceTest extends CategoryTest {
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Mock private EncryptionService encryptionService;
  @Mock private EncryptionConfig encryptionConfig;
  @Mock private EncryptedDataDetail encryptedDataDetail1;
  @Mock private EncryptedDataDetail encryptedDataDetail2;

  private ManagerDecryptionServiceImpl managerDecryptionService;
  private String accountId = UUIDGenerator.generateUuid();

  @Before
  public void setup() {
    initMocks(this);
    managerDecryptionService = new ManagerDecryptionServiceImpl(delegateProxyFactory);

    when(delegateProxyFactory.get(eq(EncryptionService.class), any(SyncTaskContext.class)))
        .thenReturn(encryptionService);

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
    List<EncryptableSettingWithEncryptionDetails> encryptableSettingWithEncryptionDetails =
        SecurityTestUtils.getEncryptableSettingWithEncryptionDetailsList(
            accountId, Lists.newArrayList(encryptedDataDetail1, encryptedDataDetail2));

    when(encryptionService.decrypt(encryptableSettingWithEncryptionDetails))
        .thenReturn(encryptableSettingWithEncryptionDetails);

    managerDecryptionService.decrypt(accountId, encryptableSettingWithEncryptionDetails);

    for (EncryptableSettingWithEncryptionDetails details : encryptableSettingWithEncryptionDetails) {
      assertThat(details.getEncryptableSetting().isDecrypted()).isEqualTo(true);
    }
  }
}
