package io.harness.cvng.verificationjob.entities;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.rule.OwnerRule.KAMAL;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.google.common.collect.Lists;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.cvng.beans.DataSourceType;
import io.harness.cvng.verificationjob.beans.Sensitivity;
import io.harness.cvng.verificationjob.entities.VerificationJob.VerificationJobKeys;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.time.Duration;
import java.util.Collections;

public class VerificationJobTest extends CategoryTest {
  private String accountId;
  @Before
  public void setup() {
    this.accountId = generateUuid();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testValidate_forRequiredFields() throws IllegalAccessException {
    testFieldForNotNull(VerificationJobKeys.accountId);
    testFieldForNotNull(VerificationJobKeys.identifier);
    testFieldForNotNull(VerificationJobKeys.jobName);
    testFieldForNotNull(VerificationJobKeys.dataSources);
    testFieldForNotNull(VerificationJobKeys.duration);
    testFieldForNotNull(VerificationJobKeys.envIdentifier);
    testFieldForNotNull(VerificationJobKeys.serviceIdentifier);
    testFieldForNotNull(VerificationJobKeys.serviceIdentifier);
    createVerificationJob().validate();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testValidate_callsValidateParams() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob = spy(verificationJob);
    verificationJob.validate();
    verify(verificationJob, times(1)).validateParams();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testValidate_shouldThrowExceptionWhenDurationWhenZero() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setDuration(Duration.ofSeconds(0));
    assertThatThrownBy(() -> verificationJob.validate())
        .hasMessage("Minimum allowed duration is 5 mins. Current value(ms): 0");
  }

  @Test
  @Owner(developers = KAMAL)
  @Category(UnitTests.class)
  public void testValidate_shouldThrowExceptionWhenDurationIsLessThen5() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setDuration(Duration.ofMinutes(4));
    assertThatThrownBy(() -> verificationJob.validate())
        .hasMessage("Minimum allowed duration is 5 mins. Current value(ms): 240000");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidate_validDuration() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setDuration(Duration.ofMinutes(5));
    verificationJob.validate();
  }

  @Test
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidate_emptyDataSources() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setDataSources(Collections.emptyList());
    assertThatThrownBy(() -> verificationJob.validate())
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("DataSources can not be empty");
  }

  @Test(expected = Test.None.class)
  @Owner(developers = KAMAL)
  @Category({UnitTests.class})
  public void testValidate_validDataSources() {
    VerificationJob verificationJob = createVerificationJob();
    verificationJob.setDataSources(Collections.singletonList(DataSourceType.SPLUNK));
    verificationJob.validate();
  }

  private void testFieldForNotNull(String fieldName) throws IllegalAccessException {
    VerificationJob verificationJob = createVerificationJob();
    FieldUtils.writeField(verificationJob, fieldName, null, true);
    assertThatThrownBy(() -> verificationJob.validate())
        .isInstanceOf(NullPointerException.class)
        .hasMessage(fieldName + " should not be null");
  }

  private VerificationJob createVerificationJob() {
    TestVerificationJob testVerificationJob = new TestVerificationJob();
    testVerificationJob.setAccountId(accountId);
    testVerificationJob.setIdentifier("identifier");
    testVerificationJob.setJobName(generateUuid());
    testVerificationJob.setDataSources(Lists.newArrayList(DataSourceType.APP_DYNAMICS));
    testVerificationJob.setSensitivity(Sensitivity.MEDIUM);
    testVerificationJob.setServiceIdentifier(generateUuid());
    testVerificationJob.setEnvIdentifier(generateUuid());
    testVerificationJob.setBaseLineVerificationTaskIdentifier(generateUuid());
    testVerificationJob.setDuration(Duration.ofMinutes(15));
    testVerificationJob.setProjectIdentifier(generateUuid());
    testVerificationJob.setOrgIdentifier(generateUuid());
    return testVerificationJob;
  }
}