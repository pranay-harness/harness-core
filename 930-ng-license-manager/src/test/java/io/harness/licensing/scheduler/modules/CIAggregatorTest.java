package io.harness.licensing.scheduler.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.licensing.entities.transactions.modules.CILicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CIAggregatorTest {
  @InjectMocks private CIAggregator ciCheckProcessor;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testPorceessTransaction() {
    CheckResult checkResult = CheckResult.newDefaultResult();
    CILicenseTransaction transaction1 = CILicenseTransaction.builder().developers(10).build();
    CILicenseTransaction transaction2 = CILicenseTransaction.builder().developers(10).build();
    ciCheckProcessor.processTransaction(checkResult, transaction1);
    ciCheckProcessor.processTransaction(checkResult, transaction2);
    assertThat(checkResult.getTotalDevelopers()).isEqualTo(20);
  }
}
