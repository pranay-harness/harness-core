package software.wings.resources;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.EntityVersion.EntityVersionBuilder.anEntityVersion;
import static software.wings.utils.WingsTestConstants.APP_ID;

import org.junit.ClassRule;
import org.junit.Test;
import software.wings.beans.EntityVersion;
import software.wings.beans.RestResponse;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.exception.WingsExceptionMapper;
import software.wings.service.intfc.EntityVersionService;
import software.wings.utils.ResourceTestRule;

import javax.ws.rs.core.GenericType;

/**
 * Created by peeyushaggarwal on 11/2/16.
 */
public class VersionResourceTest {
  private static EntityVersionService ENTITY_VERSION_SERVICE = mock(EntityVersionService.class);

  @ClassRule
  public static final ResourceTestRule RESOURCES = ResourceTestRule.builder()
                                                       .addResource(new VersionResource(ENTITY_VERSION_SERVICE))
                                                       .addProvider(WingsExceptionMapper.class)
                                                       .build();

  @Test
  public void shouldListVersions() throws Exception {
    PageResponse<EntityVersion> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(anEntityVersion().build()));
    pageResponse.setTotal(1);
    when(ENTITY_VERSION_SERVICE.listEntityVersions(any(PageRequest.class))).thenReturn(pageResponse);
    RestResponse<PageResponse<EntityVersion>> restResponse =
        RESOURCES.client()
            .target("/versions?appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<PageResponse<EntityVersion>>>() {});
    PageRequest<EntityVersion> pageRequest = new PageRequest<>();
    pageRequest.setOffset("0");
    pageRequest.setLimit("50");
    verify(ENTITY_VERSION_SERVICE).listEntityVersions(pageRequest);
    assertThat(restResponse.getResource().getResponse().size()).isEqualTo(1);
    assertThat(restResponse.getResource().getResponse().get(0)).isNotNull();
  }
}
