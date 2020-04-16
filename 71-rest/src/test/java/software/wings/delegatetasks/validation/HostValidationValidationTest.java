package software.wings.delegatetasks.validation;

import static com.google.common.collect.Streams.zip;
import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.delegate.beans.TaskData.DEFAULT_ASYNC_CALL_TIMEOUT;
import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyListOf;
import static software.wings.delegatetasks.validation.HostValidationValidation.BATCH_HOST_VALIDATION;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.FakeTimeLimiter;

import io.harness.CategoryTest;
import io.harness.beans.DelegateTask;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.TaskData;
import io.harness.rule.Owner;
import io.harness.security.encryption.EncryptedDataDetail;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import software.wings.annotation.EncryptableSetting;
import software.wings.beans.HostValidationTaskParameters;
import software.wings.beans.SettingAttribute;
import software.wings.beans.WinRmConnectionAttributes;
import software.wings.service.intfc.security.EncryptionService;

import java.time.Clock;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class HostValidationValidationTest extends CategoryTest {
  @Mock EncryptionService encryptionService;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);
    Mockito.doReturn(null)
        .when(encryptionService)
        .decrypt(any(EncryptableSetting.class), anyListOf(EncryptedDataDetail.class));
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validate_winrm_error() throws IllegalAccessException {
    final SettingAttribute connectionSetting = new SettingAttribute();
    connectionSetting.setValue(
        WinRmConnectionAttributes.builder().username("user").password("pwd".toCharArray()).build());
    final HostValidationValidation validationValidation =
        getHostValidationValidation(Collections.singletonList("localhost"), connectionSetting);
    final List<DelegateConnectionResult> validate = validationValidation.validate();
    assertThat(validate.get(0).isValidated()).isFalse();
    assertThat(validate.get(0).getCriteria()).isEqualTo(BATCH_HOST_VALIDATION + "localhost");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void validate_ssh_error() throws IllegalAccessException {
    final SettingAttribute connectionSetting = new SettingAttribute();
    final HostValidationValidation hostValidation =
        getHostValidationValidation(Collections.singletonList("localhost"), connectionSetting);
    final List<DelegateConnectionResult> validate = hostValidation.validate();
    assertThat(validate.get(0).isValidated()).isFalse();
    assertThat(validate.get(0).getCriteria()).isEqualTo(BATCH_HOST_VALIDATION + "localhost");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testGetCriteria() throws IllegalAccessException {
    final SettingAttribute connectionSetting = new SettingAttribute();
    connectionSetting.setValue(
        WinRmConnectionAttributes.builder().username("user").password("pwd".toCharArray()).build());
    final HostValidationValidation hostValidation =
        getHostValidationValidation(Arrays.asList("localhost", "server1", "server2"), connectionSetting);
    final List<String> criteriaList = hostValidation.getCriteria();
    assertThat(criteriaList)
        .contains(
            BATCH_HOST_VALIDATION + "localhost", BATCH_HOST_VALIDATION + "server1", BATCH_HOST_VALIDATION + "server2");
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(UnitTests.class)
  public void testPrepareResult() throws IllegalAccessException {
    final DelegateConnectionResult result1 =
        DelegateConnectionResult.builder().validated(true).criteria(BATCH_HOST_VALIDATION + "server1").build();
    final DelegateConnectionResult result2 =
        DelegateConnectionResult.builder().validated(false).criteria(BATCH_HOST_VALIDATION + "server2").build();
    final DelegateConnectionResult result3 =
        DelegateConnectionResult.builder().validated(true).criteria(BATCH_HOST_VALIDATION + "server3").build();

    final HostValidationValidation hostValidationValidation = getHostValidationValidation(null, null);
    final List<DelegateConnectionResult> delegateConnectionResultsInput = Arrays.asList(result1, result2, result3);
    final List<DelegateConnectionResult> delegateConnectionResults =
        hostValidationValidation.prepareResult(delegateConnectionResultsInput);
    assertThat(delegateConnectionResults.stream().allMatch(DelegateConnectionResult::isValidated)).isTrue();

    assertThat(zip(delegateConnectionResultsInput.stream(), delegateConnectionResults.stream(), Pair::of)
                   .allMatch(pair -> pair.getLeft().getCriteria().equals(pair.getRight().getCriteria())))
        .isTrue();

    final DelegateConnectionResult result4 =
        DelegateConnectionResult.builder().validated(false).criteria(BATCH_HOST_VALIDATION + "server1").build();
    final DelegateConnectionResult result5 =
        DelegateConnectionResult.builder().validated(false).criteria(BATCH_HOST_VALIDATION + "server2").build();
    final DelegateConnectionResult result6 =
        DelegateConnectionResult.builder().validated(false).criteria(BATCH_HOST_VALIDATION + "server3").build();
    final HostValidationValidation hostValidationValidation1 = getHostValidationValidation(null, null);
    final List<DelegateConnectionResult> delegateConnectionResults1 =
        hostValidationValidation1.prepareResult(Arrays.asList(result4, result5, result6));
    assertThat(delegateConnectionResults1.stream().anyMatch(DelegateConnectionResult::isValidated)).isFalse();
  }

  private HostValidationValidation getHostValidationValidation(
      List<String> hostNames, SettingAttribute connectionSetting) throws IllegalAccessException {
    HostValidationValidation hostValidationValidation = new HostValidationValidation(generateUuid(),
        DelegateTask.builder()
            .data(TaskData.builder()
                      .async(true)
                      .parameters(new Object[] {
                          HostValidationTaskParameters.builder()
                              .hostNames(hostNames)
                              .encryptionDetails(Lists.newArrayList(EncryptedDataDetail.builder().build()))
                              .connectionSetting(connectionSetting)
                              .build(),
                          null, hostNames})
                      .timeout(DEFAULT_ASYNC_CALL_TIMEOUT)
                      .build())
            .build(),
        null);

    FieldUtils.writeField(hostValidationValidation, "encryptionService", encryptionService, true);
    FieldUtils.writeField(hostValidationValidation, "timeLimiter", new FakeTimeLimiter(), true);
    FieldUtils.writeField(hostValidationValidation, "clock", Clock.systemUTC(), true);

    return hostValidationValidation;
  }
}