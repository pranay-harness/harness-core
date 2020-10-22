package software.wings.resources;

import static io.harness.rule.OwnerRule.NIKUNJ;
import static java.lang.String.format;
import static javax.ws.rs.client.Entity.entity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.io.ByteStreams;
import com.google.common.io.Files;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ccm.views.entities.CEReportSchedule;
import io.harness.ccm.views.service.CEReportScheduleService;
import io.harness.rule.Owner;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.TemporaryFolder;
import org.springframework.scheduling.support.CronSequenceGenerator;
import software.wings.utils.ResourceTestRule;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

public class CEReportScheduleResourceTest extends CategoryTest {
  private static CEReportScheduleService ceReportScheduleService = mock(CEReportScheduleService.class);
  @ClassRule
  public static ResourceTestRule RESOURCES =
      ResourceTestRule.builder().instance(new CEReportScheduleResource(ceReportScheduleService)).build();
  private final String ACCOUNT_ID = "ACCOUNT_ID";
  private final String NAME = "REPORT_NAME";
  private final String REPORT_ID = "REPORT_ID";
  private final String[] RECIPIENTS = {"user1@harness.io"};
  private final String[] VIEWS_ID = {"ceviewsid123"};
  private final String USER_CRON = "* 30 12 * * *"; // 12.30PM daily
  private final String[] RECIPIENTS2 = {"user2@harness.io"};
  @Rule public TemporaryFolder tempFolder = new TemporaryFolder(new File("/tmp"));
  private File tempFile;
  private CEReportSchedule reportSchedule;

  @Before
  public void setUp() throws IllegalAccessException, IOException {
    CEReportScheduleResource instance = (CEReportScheduleResource) RESOURCES.getInstances().iterator().next();
    FieldUtils.writeField(instance, "ceReportScheduleService", ceReportScheduleService, true);
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
    tempFile = tempFolder.newFile();
    Files.write(
        "{\"metaData\":{},\"resource\":[{\"uuid\":\"REPORT_ID\",\"name\":\"REPORT_NAME\",\"enabled\":true,\"description\":\"\",\"viewsId\":[\"ceviewsid123\"],\"userCron\":\"* 30 12 * * *\",\"recipients\":[\"user1@harness.io\"],\"accountId\":\"ACCOUNT_ID\",\"createdAt\":0,\"lastUpdatedAt\":0,\"createdBy\":null,\"lastUpdatedBy\":null,\"nextExecution\":null}],\"responseMessages\":[]}"
            .getBytes(),
        tempFile);

    when(ceReportScheduleService.get(REPORT_ID, ACCOUNT_ID)).thenReturn(reportSchedule);
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testGet() throws IOException {
    Response r = RESOURCES.client()
                     .target(format("/ceReportSchedule/%s/?reportId=%s", ACCOUNT_ID, REPORT_ID))
                     .request()
                     .get(new GenericType<Response>() {});

    assertThat(tempFile).hasContent(new String(ByteStreams.toByteArray((InputStream) r.getEntity())));
    verify(ceReportScheduleService).get(REPORT_ID, ACCOUNT_ID);
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testCreateReportSetting() {
    RESOURCES.client()
        .target(format("/ceReportSchedule/%s/", ACCOUNT_ID))
        .request()
        .post(entity(reportSchedule, MediaType.APPLICATION_JSON), new GenericType<Response>() {});
    CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(reportSchedule.getUserCron());
    verify(ceReportScheduleService).createReportSetting(cronSequenceGenerator, ACCOUNT_ID, reportSchedule);
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testCreateReportSettingWithoutName() {
    reportSchedule.setName("");
    Response r = RESOURCES.client()
                     .target(format("/ceReportSchedule/%s/", ACCOUNT_ID))
                     .request()
                     .post(entity(reportSchedule, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    assertThat(r.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testCreateReportSettingWithoutViewId() {
    reportSchedule.setViewsId(null);
    Response r = RESOURCES.client()
                     .target(format("/ceReportSchedule/%s/", ACCOUNT_ID))
                     .request()
                     .post(entity(reportSchedule, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    assertThat(r.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testCreateReportSettingWithoutRecipients() {
    reportSchedule.setRecipients(null);
    Response r = RESOURCES.client()
                     .target(format("/ceReportSchedule/%s/", ACCOUNT_ID))
                     .request()
                     .post(entity(reportSchedule, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    assertThat(r.getStatus()).isEqualTo(Response.Status.BAD_REQUEST.getStatusCode());
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testModifyRecipients() {
    reportSchedule.setRecipients(RECIPIENTS2);
    RESOURCES.client()
        .target(format("/ceReportSchedule/%s/", ACCOUNT_ID))
        .request()
        .put(entity(reportSchedule, MediaType.APPLICATION_JSON), new GenericType<Response>() {});

    CronSequenceGenerator cronSequenceGenerator = new CronSequenceGenerator(reportSchedule.getUserCron());
    verify(ceReportScheduleService).update(cronSequenceGenerator, ACCOUNT_ID, reportSchedule);
  }

  @Test
  @Owner(developers = NIKUNJ)
  @Category(UnitTests.class)
  public void testDeleteReportSetting() {
    RESOURCES.client()
        .target(format("/ceReportSchedule/%s?reportId=%s", ACCOUNT_ID, REPORT_ID))
        .request()
        .delete(new GenericType<Response>() {});
    verify(ceReportScheduleService).delete(REPORT_ID, ACCOUNT_ID);
  }
}
