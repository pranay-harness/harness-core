package io.harness.licensing.scheduler.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.licensing.entities.transactions.modules.CFLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CFAggregatorTest {
  @InjectMocks private CFAggregator cfCheckProcessor;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testPorceessTransaction() {
    CheckResult checkResult = CheckResult.newDefaultResult();
    CFLicenseTransaction transaction1 = CFLicenseTransaction.builder().featureFlagUnit(10).clientMAU(15L).build();
    CFLicenseTransaction transaction2 = CFLicenseTransaction.builder().featureFlagUnit(10).clientMAU(15L).build();
    cfCheckProcessor.processTransaction(checkResult, transaction1);
    cfCheckProcessor.processTransaction(checkResult, transaction2);
    assertThat(checkResult.getTotalFeatureFlagUnit()).isEqualTo(20);
    assertThat(checkResult.getTotalClientMAU()).isEqualTo(30L);
  }
}
