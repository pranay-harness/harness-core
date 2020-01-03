package software.wings.delegatetasks.validation;

import static io.harness.rule.OwnerRule.UTKARSH;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.data.structure.UUIDGenerator;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.security.encryption.EncryptionConfig;
import io.harness.security.encryption.EncryptionType;
import lombok.Data;
import net.openhft.chronicle.core.util.Time;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * @author marklu on 2019-06-12
 */
public class SecretManagerValidationTest extends CategoryTest {
  @Data
  public static class TestSecretManagerValidation extends AbstractSecretManagerValidation {
    Object[] parameters;

    TestSecretManagerValidation(
        String delegateId, DelegateTask delegateTask, Consumer<List<DelegateConnectionResult>> postExecute) {
      super(delegateId, delegateTask, postExecute);
    }
  }

  private TestSecretManagerValidation validation;

  @Before
  public void setUp() {
    TaskData taskData = mock(TaskData.class);
    DelegateTask delegateTask = mock(DelegateTask.class);
    when(delegateTask.getData()).thenReturn(taskData);

    validation = new TestSecretManagerValidation(UUIDGenerator.generateUuid(), delegateTask, null);
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidationWithNullEncryptionType() {
    validation.setParameters(new Object[] {});
    DelegateConnectionResult result = validation.validateSecretManager();
    assertThat(result).isNotNull();
    assertThat(result.isValidated()).isTrue();
    assertThat(result.getCriteria().contains(EncryptionType.LOCAL.name())).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidationWithEncryptionConfig() {
    EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
    when(encryptionConfig.getEncryptionServiceUrl()).thenReturn("https://www.google.com");
    validation.setParameters(new Object[] {encryptionConfig});
    int retries = 0;
    DelegateConnectionResult result = validation.validateSecretManager();
    assertThat(result).isNotNull();
    while (retries < 8 && !result.isValidated()) {
      Time.sleep(3, TimeUnit.SECONDS);
      retries++;
      result = validation.validateSecretManager();
    }
    assertThat(result.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidationWithEncryptedDataDetails() {
    EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
    EncryptedDataDetail encryptedDataDetail = mock(EncryptedDataDetail.class);
    when(encryptionConfig.getEncryptionServiceUrl()).thenReturn("https://www.google.com");
    when(encryptedDataDetail.getEncryptionConfig()).thenReturn(encryptionConfig);
    validation.setParameters(new Object[] {encryptedDataDetail});
    int retries = 0;
    DelegateConnectionResult result = validation.validateSecretManager();
    assertThat(result).isNotNull();
    while (retries < 8 && !result.isValidated()) {
      Time.sleep(3, TimeUnit.SECONDS);
      retries++;
      result = validation.validateSecretManager();
    }
    assertThat(result.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidationWithEncryptedDataDetailsList() {
    EncryptionConfig encryptionConfig = mock(EncryptionConfig.class);
    EncryptedDataDetail encryptedDataDetail = mock(EncryptedDataDetail.class);
    when(encryptionConfig.getEncryptionServiceUrl()).thenReturn("https://www.google.com");
    when(encryptedDataDetail.getEncryptionConfig()).thenReturn(encryptionConfig);
    validation.setParameters(new Object[] {Arrays.asList(encryptedDataDetail)});
    int retries = 0;
    DelegateConnectionResult result = validation.validateSecretManager();
    assertThat(result).isNotNull();
    while (retries < 8 && !result.isValidated()) {
      Time.sleep(3, TimeUnit.SECONDS);
      retries++;
      result = validation.validateSecretManager();
    }
    assertThat(result.isValidated()).isTrue();
  }

  @Test
  @Owner(developers = UTKARSH)
  @Category(UnitTests.class)
  public void testValidationWithListOfNonEncryptedObjects() {
    validation.setParameters(new Object[] {Arrays.asList(new Object(), new Object())});
    DelegateConnectionResult result = validation.validateSecretManager();
    assertThat(result).isNotNull();
    assertThat(result.isValidated()).isTrue();
  }
}
