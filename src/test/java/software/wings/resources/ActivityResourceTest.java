package software.wings.resources;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.CommandUnitType.EXEC;
import static software.wings.beans.ExecCommandUnit.Builder.anExecCommandUnit;
import static software.wings.beans.Log.Builder.aLog;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.utils.WingsTestConstants.ACTIVITY_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.COMMAND_UNIT_NAME;
import static software.wings.utils.WingsTestConstants.ENV_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.rules.Verifier;
import org.mockito.ArgumentCaptor;
import software.wings.beans.Activity;
import software.wings.beans.CommandUnit;
import software.wings.beans.ExecCommandUnit;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import java.util.List;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;

// TODO: Auto-generated Javadoc

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class ActivityResourceTest {
  /**
   * The constant ACTIVITY_SERVICE.
   */
  public static final ActivityService ACTIVITY_SERVICE = mock(ActivityService.class);
  /**
   * The constant LOG_SERVICE.
   */
  public static final LogService LOG_SERVICE = mock(LogService.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new ActivityResource(ACTIVITY_SERVICE, LOG_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  /**
   * The constant ACTUAL_ACTIVITY.
   */
  public static final Activity ACTUAL_ACTIVITY = anActivity().build();
  /**
   * The constant ACTUAL_LOG.
   */
  public static final Log ACTUAL_LOG = aLog().build();

  /**
   * The Test folder.
   */
  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(ACTIVITY_SERVICE, LOG_SERVICE);
    }
  };

  /**
   * Sets the up.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setUp() throws IOException {
    reset(ACTIVITY_SERVICE, LOG_SERVICE);
    PageResponse<Activity> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Lists.newArrayList(ACTUAL_ACTIVITY));
    pageResponse.setTotal(1);
    when(ACTIVITY_SERVICE.list(anyString(), anyString(), any(PageRequest.class))).thenReturn(pageResponse);
    when(ACTIVITY_SERVICE.get(anyString(), anyString())).thenReturn(ACTUAL_ACTIVITY);
    PageResponse<Log> logPageResponse = new PageResponse<>();
    logPageResponse.setResponse(Lists.newArrayList(ACTUAL_LOG));
    logPageResponse.setTotal(1);
    when(LOG_SERVICE.list(any(PageRequest.class))).thenReturn(logPageResponse);
  }

  /**
   * Should list activities.
   */
  @Test
  public void shouldListActivities() {
    RestResponse<PageResponse<Activity>> restResponse =
        RESOURCES.client()
            .target("/activities?appId=" + APP_ID + "&envId=" + ENV_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<Activity>>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(PageResponse.class);
    PageRequest<Activity> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.setOffset("0");
    expectedPageRequest.setLimit("50");

    verify(ACTIVITY_SERVICE).list(APP_ID, ENV_ID, expectedPageRequest);
  }

  /**
   * Should get activity.
   */
  @Test
  public void shouldGetActivity() {
    RestResponse<Activity> restResponse = RESOURCES.client()
                                              .target("/activities/" + ACTIVITY_ID + "?appId=" + APP_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Activity>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(Activity.class);

    verify(ACTIVITY_SERVICE).get(ACTIVITY_ID, APP_ID);
  }

  /**
   * Should list logs.
   */
  @Test
  public void shouldListLogs() {
    RestResponse<PageResponse<Log>> restResponse =
        RESOURCES.client()
            .target(String.format("/activities/%s/logs?appId=%s&unitName=%s", ACTIVITY_ID, APP_ID, COMMAND_UNIT_NAME))
            .request()
            .get(new GenericType<RestResponse<PageResponse<Log>>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(PageResponse.class);

    ArgumentCaptor<PageRequest> argument = ArgumentCaptor.forClass(PageRequest.class);
    verify(LOG_SERVICE).list(argument.capture());

    List<SearchFilter> filters = argument.getValue().getFilters();
    assertThatFilterMatches(filters.get(0), "appId", APP_ID, EQ);
    assertThatFilterMatches(filters.get(1), "activityId", ACTIVITY_ID, EQ);
    assertThatFilterMatches(filters.get(2), "commandUnitName", COMMAND_UNIT_NAME, EQ);
  }

  /**
   * Should list command units.
   */
  @Test
  public void shouldListCommandUnits() {
    when(ACTIVITY_SERVICE.getCommandUnits(APP_ID, ACTIVITY_ID))
        .thenReturn(asList(anExecCommandUnit()
                               .withName(COMMAND_UNIT_NAME)
                               .withCommandUnitType(EXEC)
                               .withCommandString("./bin/start.sh")
                               .build()));

    RestResponse<List<ExecCommandUnit>> restResponse =
        RESOURCES.client()
            .target(String.format("/activities/%s/units?appId=%s", ACTIVITY_ID, APP_ID))
            .request()
            .get(new GenericType<RestResponse<List<ExecCommandUnit>>>() {});
    assertThat(restResponse.getResource()).isInstanceOf(List.class);
    assertThat(restResponse.getResource().size()).isEqualTo(1);
    assertThat(restResponse.getResource().get(0)).isInstanceOf(CommandUnit.class);
    verify(ACTIVITY_SERVICE).getCommandUnits(APP_ID, ACTIVITY_ID);
  }

  /**
   * Should download activity log file.
   *
   * @throws IOException the io exception
   */
  @Test
  public void shouldDownloadActivityLogFile() throws IOException {
    when(LOG_SERVICE.exportLogs(APP_ID, ACTIVITY_ID)).thenReturn(testFolder.newFile("FILE_NAME"));
    Response response = RESOURCES.client()
                            .target(String.format("/activities/%s/all-logs?appId=%s", ACTIVITY_ID, APP_ID))
                            .request()
                            .get();
    assertThat(response.getStatus()).isEqualTo(200);
    assertThat(response.getHeaderString("Content-Disposition")).isEqualTo("attachment; filename=FILE_NAME");
    assertThat(response.getHeaderString("Content-type")).isEqualTo("text/plain");
    verify(LOG_SERVICE).exportLogs(APP_ID, ACTIVITY_ID);
  }

  private void assertThatFilterMatches(SearchFilter filter, String name, String value, Operator op) {
    assertThat(filter.getFieldName()).isEqualTo(name);
    assertThat(filter.getFieldValues()[0]).isEqualTo(value);
    assertThat(filter.getOp()).isEqualTo(op);
  }
}
