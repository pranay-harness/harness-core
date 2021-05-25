package io.harness.licensing.scheduler.modules;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.category.element.UnitTests;
import io.harness.licensing.entities.transactions.modules.CDLicenseTransaction;
import io.harness.licensing.scheduler.CheckResult;
import io.harness.rule.Owner;

import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;

public class CDAggregatorTest {
  @InjectMocks private CDAggregator cdCheckProcessor;

  @Before
  public void setup() {
    initMocks(this);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testPorceessTransaction() {
    CheckResult checkResult = CheckResult.newDefaultResult();
    CDLicenseTransaction transaction1 = CDLicenseTransaction.builder().uuid("expire").workload(50).build();
    cdCheckProcessor.processTransaction(checkResult, transaction1);
    CDLicenseTransaction transaction2 = CDLicenseTransaction.builder().uuid("expire").workload(50).build();
    cdCheckProcessor.processTransaction(checkResult, transaction2);
    assertThat(checkResult.getTotalWorkload()).isEqualTo(100);
  }
}
