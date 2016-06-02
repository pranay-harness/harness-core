package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.Activity.Builder.anActivity;
import static software.wings.beans.Log.Builder.aLog;

import com.google.common.collect.Lists;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import software.wings.beans.Activity;
import software.wings.beans.Log;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.ActivityService;
import software.wings.service.intfc.LogService;

import java.io.IOException;
import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 4/1/16.
 */
public class ActivityResourceTest {
  public static final ActivityService ACTIVITY_SERVICE = mock(ActivityService.class);
  public static final LogService LOG_SERVICE = mock(LogService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new ActivityResource(ACTIVITY_SERVICE, LOG_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  public static final String APP_ID = "APP_ID";
  public static final String ACTIVITY_ID = "ACTIVITY_ID";
  public static final String ENV_ID = "ENV_ID";

  public static final Activity ACTUAL_ACTIVITY = anActivity().build();
  public static final Log ACTUAL_LOG = aLog().build();

  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(ACTIVITY_SERVICE, LOG_SERVICE);
    }
  };

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

  @Test
  public void shouldGetActivity() {
    RestResponse<Activity> restResponse = RESOURCES.client()
                                              .target("/activities/" + ACTIVITY_ID + "?appId=" + APP_ID)
                                              .request()
                                              .get(new GenericType<RestResponse<Activity>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(Activity.class);

    verify(ACTIVITY_SERVICE).get(ACTIVITY_ID, APP_ID);
  }

  @Test
  public void shouldListLogs() {
    RestResponse<PageResponse<Log>> restResponse = RESOURCES.client()
                                                       .target("/activities/" + ACTIVITY_ID + "/logs?appId=" + APP_ID)
                                                       .request()
                                                       .get(new GenericType<RestResponse<PageResponse<Log>>>() {});

    assertThat(restResponse.getResource()).isInstanceOf(PageResponse.class);
    PageRequest<Log> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", APP_ID, Operator.EQ);
    expectedPageRequest.addFilter("activityId", ACTIVITY_ID, Operator.EQ);
    expectedPageRequest.setOffset("0");
    expectedPageRequest.setLimit("50");

    verify(LOG_SERVICE).list(expectedPageRequest);
  }
}
