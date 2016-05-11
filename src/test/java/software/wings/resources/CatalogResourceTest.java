/**
 *
 */
package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.RestResponse;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.WorkflowService;

import java.util.ArrayList;
import java.util.Map;

import javax.ws.rs.core.GenericType;

/**
 * @author Rishi
 *
 */
public class CatalogResourceTest extends WingsBaseTest {
  private static final CatalogService catalogService = mock(CatalogService.class);
  private static final WorkflowService workflowService = mock(WorkflowService.class);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder().addResource(new CatalogResource(catalogService, workflowService)).build();

  @After
  public void tearDown() {
    reset(catalogService, workflowService);
  }

  @Test
  public void shouldListCatalogs() {
    when(catalogService.getCatalogItems(anyString())).thenReturn(new ArrayList<>());
    when(workflowService.stencils()).thenReturn(new ArrayList<>());

    RestResponse<Map<String, Object>> actual =
        resources.client()
            .target("/catalogs?catalogType=ORCHESTRATION_STENCILS&catalogType=CARD_VIEW_SORT_BY")
            .request()
            .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual).isNotNull();
    assertThat(actual.getResource()).isNotNull();
    assertThat(actual.getResource().size()).isEqualTo(2);
    assertThat(actual.getResource().get("ORCHESTRATION_STENCILS")).isNotNull();
    assertThat(actual.getResource().get("CARD_VIEW_SORT_BY")).isNotNull();
  }
}
