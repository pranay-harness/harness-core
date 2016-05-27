package software.wings.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import com.google.common.collect.Lists;

import io.dropwizard.testing.junit.ResourceTestRule;
import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import software.wings.WingsBaseTest;
import software.wings.beans.CatalogNames;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;

/**
 * @author Rishi.
 */
public class CatalogResourceTest extends WingsBaseTest {
  private static final CatalogService catalogService = mock(CatalogService.class);
  private static final WorkflowService workflowService = mock(WorkflowService.class);
  private static final JenkinsBuildService jenkinsBuildService = mock(JenkinsBuildService.class);
  private static final SettingsService settingsService = mock(SettingsService.class);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder()
          .addResource(new CatalogResource(catalogService, workflowService, jenkinsBuildService, settingsService))
          .build();

  @After
  public void tearDown() {
    reset(catalogService, workflowService);
  }

  @Test
  public void shouldListCatalogs() {
    when(catalogService.getCatalogItems(anyString())).thenReturn(new ArrayList<>());
    HashMap<StateTypeScope, List<StateTypeDescriptor>> stencils =
        new HashMap<StateTypeScope, List<StateTypeDescriptor>>();
    stencils.put(StateTypeScope.ORCHESTRATION_STENCILS, new ArrayList<>());
    when(workflowService.stencils(StateTypeScope.ORCHESTRATION_STENCILS)).thenReturn(stencils);

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

  @Test
  public void shouldListCatalogsForJenkinsBuild() throws IOException {
    when(jenkinsBuildService.getBuilds(any(MultivaluedMap.class))).thenReturn(Lists.newArrayList());

    RestResponse<Map<String, Object>> actual = resources.client()
                                                   .target("/catalogs?catalogType=JENKINS_BUILD")
                                                   .request()
                                                   .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual)
        .isNotNull()
        .extracting(RestResponse::getResource)
        .hasSize(1)
        .extracting(o -> ((Map<String, Object>) o).get(CatalogNames.JENKINS_BUILD))
        .isNotNull();
  }

  @Test
  public void shouldListCatalogForConnectionAttributes() {
    when(settingsService.getSettingAttributesByType(anyString(), any(SettingVariableTypes.class)))
        .thenReturn(Lists.newArrayList());
    RestResponse<Map<String, Object>> actual = resources.client()
                                                   .target("/catalogs?catalogType=CONNECTION_ATTRIBUTES")
                                                   .request()
                                                   .get(new GenericType<RestResponse<Map<String, Object>>>() {});
    assertThat(actual).isNotNull();
    assertThat(actual.getResource().size()).isEqualTo(1);
    assertThat(actual.getResource().get("CONNECTION_ATTRIBUTES")).isNotNull();
  }

  @Test
  public void shouldListCatalogForBastionHostAttributes() {
    when(settingsService.getSettingAttributesByType(anyString(), any(SettingVariableTypes.class)))
        .thenReturn(Lists.newArrayList());
    RestResponse<Map<String, Object>> actual = resources.client()
                                                   .target("/catalogs?catalogType=BASTION_HOST_ATTRIBUTES")
                                                   .request()
                                                   .get(new GenericType<RestResponse<Map<String, Object>>>() {});
    assertThat(actual).isNotNull();
    assertThat(actual.getResource().size()).isEqualTo(1);
    assertThat(actual.getResource().get("BASTION_HOST_ATTRIBUTES")).isNotNull();
  }
}
