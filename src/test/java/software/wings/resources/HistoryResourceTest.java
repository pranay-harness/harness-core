package software.wings.resources;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;

import com.google.common.collect.Lists;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.History;
import software.wings.beans.RestResponse;
import software.wings.beans.SearchFilter.Operator;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.HistoryService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 6/20/16.
 */
public class HistoryResourceTest {
  public static final HistoryService HISTORY_SERVICE = mock(HistoryService.class);

  public static final HistoryResource HISTORY_RESOURCE = new HistoryResource(HISTORY_SERVICE);

  @ClassRule
  public static final ResourceTestRule RESOURCES =
      ResourceTestRule.builder().addResource(HISTORY_RESOURCE).addProvider(WingsExceptionMapper.class).build();

  @Before
  public void setUp() {
    reset(HISTORY_SERVICE);
    PageResponse<History> pageResponse = new PageResponse<>();
    pageResponse.setResponse(Lists.newArrayList());
    when(HISTORY_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
  }

  @Test
  public void shouldList() throws Exception {
    RestResponse<PageResponse<History>> restResponse =
        RESOURCES.client()
            .target("/history/?appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<History>>>() {});

    PageRequest<History> expectedPageRequest = new PageRequest<>();
    expectedPageRequest.addFilter("appId", APP_ID, Operator.EQ);
    expectedPageRequest.setOffset("0");
    expectedPageRequest.setLimit("50");
    verify(HISTORY_SERVICE).list(expectedPageRequest);
  }
}
