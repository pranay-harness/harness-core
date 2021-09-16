package io.harness.ccm.remote.resources;

import static io.harness.rule.OwnerRule.SHUBHANSHU;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.rule.Owner;

import java.io.File;
import java.io.IOException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;

public class PerspectiveReportResourceTest extends CategoryTest {
  private CEReportScheduleService ceReportScheduleService = mock(CEReportScheduleService.class);
  private PerspectiveReportResource perspectiveReportResource;

  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String NAME = "REPORT_NAME";
  private final String REPORT_ID = "REPORT_ID";
  private final String[] RECIPIENTS = {"user1@harness.io"};
  private final String[] VIEWS_ID = {"ceviewsid123"};
  private final String USER_CRON = "* 30 12 * * *"; // 12.30PM daily
  private final String[] RECIPIENTS2 = {"user2@harness.io"};
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder(new File("/tmp"));
  private CEReportSchedule reportSchedule;

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    reportSchedule = CEReportSchedule.builder()
                         .accountId(ACCOUNT_ID)
                         .viewsId(VIEWS_ID)
                         .recipients(RECIPIENTS)
                         .description("")
                         .userCron(USER_CRON)
                         .name(NAME)
                         .uuid(REPORT_ID)
                         .enabled(true)
                         .build();

    when(ceReportScheduleService.get(REPORT_ID, ACCOUNT_ID)).thenReturn(reportSchedule);
    perspectiveReportResource = new PerspectiveReportResource(ceReportScheduleService);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testGet() {
    perspectiveReportResource.getReportSetting(null, REPORT_ID, ACCOUNT_ID);
    verify(ceReportScheduleService).get(REPORT_ID, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testCreateReportSetting() {
    perspectiveReportResource.createReportSetting(ACCOUNT_ID, reportSchedule);
    verify(ceReportScheduleService).createReportSetting(ACCOUNT_ID, reportSchedule);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testModifyRecipients() {
    reportSchedule.setRecipients(RECIPIENTS2);
    perspectiveReportResource.updateReportSetting(ACCOUNT_ID, reportSchedule);
    verify(ceReportScheduleService).update(ACCOUNT_ID, reportSchedule);
  }

  @Test
  @Owner(developers = SHUBHANSHU)
  @Category(UnitTests.class)
  public void testDeleteReportSetting() {
    perspectiveReportResource.deleteReportSetting(REPORT_ID, null, ACCOUNT_ID);
    verify(ceReportScheduleService).delete(REPORT_ID, ACCOUNT_ID);
  }
}