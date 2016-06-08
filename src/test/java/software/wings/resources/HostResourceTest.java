package software.wings.resources;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.HOST_NAME;
import static software.wings.utils.WingsTestConstants.INFRA_ID;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.Host;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.HostService;

import javax.ws.rs.core.GenericType;

/**
 * Created by anubhaw on 6/7/16.
 */
public class HostResourceTest extends WingsBaseTest {
  private static final HostService RESOURCE_SERVICE = mock(HostService.class);

  /**
   * The constant RESOURCES.
   */
  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new HostResource(RESOURCE_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();
  private static final Host aHost =
      Host.HostBuilder.aHost().withAppId(APP_ID).withInfraId(INFRA_ID).withHostName(HOST_NAME).build();

  /**
   * Should list hosts.
   */
  @Test
  @Ignore
  public void shouldListHosts() {
    PageResponse<Host> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(aHost));
    pageResponse.setTotal(1);
    when(RESOURCE_SERVICE.list(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<Host>> restResponse = RESOURCES.client()
                                                        .target(format("/hosts?appId=%s&envId=%s", APP_ID, ENV_ID))
                                                        .request()
                                                        .get(new GenericType<RestResponse<PageResponse<Host>>>() {});
    PageRequest<Host> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    pageRequest.setLimit("50");
    pageRequest.addFilter("appId", APP_ID, EQ);
    pageRequest.addFilter("envId", ENV_ID, EQ);
    verify(RESOURCE_SERVICE).list(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }
}
