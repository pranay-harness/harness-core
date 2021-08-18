package io.harness.ccm.remote.resources;

import static io.harness.rule.OwnerRule.DEEPAK;

import static org.powermock.api.mockito.PowerMockito.mockStatic;

import io.harness.CategoryTest;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.maintenance.MaintenanceController;
import io.harness.rule.Owner;
import io.harness.timescaledb.tables.OverviewDashboard;
import io.harness.timescaledb.tables.records.OverviewDashboardRecord;

import java.time.OffsetDateTime;
import lombok.extern.slf4j.Slf4j;
import org.jooq.DSLContext;
import org.jooq.impl.DSL;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MaintenanceController.class)
@OwnedBy(HarnessTeam.CE)
@Slf4j
public class OverviewDashboardTest extends CategoryTest {
  DSLContext dslContext = DSL.using("jdbc:postgresql://34.83.25.129:5432/harnessdev", "harnessappdev", "harnessappdev");

  @Before
  public void setup() {
    mockStatic(MaintenanceController.class);
  }

  @Test
  @Owner(developers = DEEPAK)
  @Category(UnitTests.class)
  public void playWithJooq() throws Exception {
    final OverviewDashboardRecord overviewDashboardRecord = new OverviewDashboardRecord()
                                                                .setAccountid("accountId")
                                                                .setCpu(8.0)
                                                                .setEndtime(OffsetDateTime.now())
                                                                .setInstanceid("instanceId")
                                                                .setInstancetype("instanceType");

    dslContext.newRecord(OverviewDashboard.OVERVIEW_DASHBOARD, overviewDashboardRecord).insert();
  }
}
