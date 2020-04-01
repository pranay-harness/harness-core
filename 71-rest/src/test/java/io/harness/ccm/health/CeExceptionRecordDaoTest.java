package io.harness.ccm.health;

import static io.harness.rule.OwnerRule.HANTANG;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.WingsBaseTest;

public class CeExceptionRecordDaoTest extends WingsBaseTest {
  private String accountId = "ACCOUNT_ID";
  private String clusterId = "CLUSTER_ID";

  private CeExceptionRecord exception;
  @Inject CeExceptionRecordDao ceExceptionRecordDao;

  @Before
  public void setUp() {
    exception = CeExceptionRecord.builder().accountId(accountId).clusterId(clusterId).message("Exception").build();
  }

  @Test
  @Owner(developers = HANTANG)
  @Category(UnitTests.class)
  public void shouldSaveAndGet() {
    ceExceptionRecordDao.save(exception);
    CeExceptionRecord savedException = ceExceptionRecordDao.getLatestException(accountId, clusterId);
    assertThat(savedException).isEqualToIgnoringGivenFields(exception, "uuid");
  }
}
