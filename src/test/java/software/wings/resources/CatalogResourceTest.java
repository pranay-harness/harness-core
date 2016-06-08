package software.wings.resources;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.collect.ImmutableMap.of;
import static com.google.common.collect.Lists.newArrayList;
import static com.google.common.collect.Maps.newHashMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.CatalogNames.ORCHESTRATION_STENCILS;
import static software.wings.beans.SettingAttribute.SettingAttributeBuilder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import io.dropwizard.testing.junit.ResourceTestRule;
import junitparams.JUnitParamsRunner;
import junitparams.Parameters;
import junitparams.naming.TestCaseName;
import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Verifier;
import org.junit.runner.RunWith;
import software.wings.WingsBaseTest;
import software.wings.beans.CatalogNames;
import software.wings.beans.JenkinsConfig;
import software.wings.beans.RestResponse;
import software.wings.beans.SettingValue.SettingVariableTypes;
import software.wings.service.intfc.CatalogService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.WorkflowService;
import software.wings.sm.StateType;
import software.wings.sm.StateTypeDescriptor;
import software.wings.sm.StateTypeScope;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MultivaluedMap;

// TODO: Auto-generated Javadoc

/**
 * The Class CatalogResourceTest.
 *
 * @author Rishi.
 */
@RunWith(JUnitParamsRunner.class)
public class CatalogResourceTest extends WingsBaseTest {
  private static final CatalogService catalogService = mock(CatalogService.class);
  private static final WorkflowService workflowService = mock(WorkflowService.class);
  private static final JenkinsBuildService jenkinsBuildService = mock(JenkinsBuildService.class);
  private static final SettingsService settingsService = mock(SettingsService.class);
  private static final ServiceResourceService serviceResourceService = mock(ServiceResourceService.class);
  private static final EnvironmentService environmentService = mock(EnvironmentService.class);

  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder()
          .addResource(new CatalogResource(catalogService, workflowService, jenkinsBuildService, settingsService,
              serviceResourceService, environmentService))
          .build();

  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(catalogService, workflowService, jenkinsBuildService, settingsService,
          serviceResourceService, environmentService);
    }
  };

  /**
   * Setup mocks.
   *
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @Before
  public void setupMocks() throws IOException {
    when(settingsService.get(anyString())).thenReturn(aSettingAttribute().withValue(new JenkinsConfig()).build());
    when(jenkinsBuildService.getBuilds(any(MultivaluedMap.class), any(JenkinsConfig.class))).thenReturn(newArrayList());
    when(settingsService.getSettingAttributesByType(anyString(), any(SettingVariableTypes.class)))
        .thenReturn(newArrayList());
    when(serviceResourceService.getCommandStencils(APP_ID, SERVICE_ID)).thenReturn(newArrayList());
    when(environmentService.listForEnum(APP_ID)).thenReturn(of(ENV_ID, "ENV"));
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    reset(catalogService, workflowService, jenkinsBuildService, settingsService, serviceResourceService,
        environmentService);
  }

  /**
   * Should list catalogs.
   */
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
    assertThat(actual.getResource()).isNotNull().hasSize(2).containsKeys(ORCHESTRATION_STENCILS, "CARD_VIEW_SORT_BY");
  }

  private Object[][] catalogNames() {
    return new Object[][] {{UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.JENKINS_BUILD)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.JENKINS_CONFIG)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.CONNECTION_ATTRIBUTES)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.COMMAND_STENCILS)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.BASTION_HOST_ATTRIBUTES)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.ENVIRONMENTS)}

    };
  }

  /**
   * Should list catalogs for.
   *
   * @param catalogNameForDisplay the catalog name for display
   */
  @Test
  @TestCaseName("{method}{0}")
  @Parameters(method = "catalogNames")
  public void shouldListCatalogsFor(String catalogNameForDisplay) {
    String catalogName = UPPER_CAMEL.to(UPPER_UNDERSCORE, catalogNameForDisplay);
    RestResponse<Map<String, Object>> actual = resources.client()
                                                   .target("/catalogs?catalogType=" + catalogName + "&appId=" + APP_ID)
                                                   .request()
                                                   .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual)
        .isNotNull()
        .extracting(RestResponse::getResource)
        .hasSize(1)
        .extracting(o -> ((Map<String, Object>) o).get(catalogName))
        .isNotNull();
  }

  /**
   * Should list catalogs for command stencils and service.
   */
  @Test
  public void shouldListCatalogsForCommandStencilsAndService() {
    RestResponse<Map<String, Object>> actual = resources.client()
                                                   .target("/catalogs?catalogType=" + CatalogNames.COMMAND_STENCILS
                                                       + "&appId=" + APP_ID + "&serviceId=" + SERVICE_ID)
                                                   .request()
                                                   .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual)
        .isNotNull()
        .extracting(RestResponse::getResource)
        .hasSize(1)
        .extracting(o -> ((Map<String, Object>) o).get(CatalogNames.COMMAND_STENCILS))
        .isNotNull();
    verify(serviceResourceService).getCommandStencils(APP_ID, SERVICE_ID);
  }

  @Test
  public void shouldListStencilsWithPostProcessing() {
    when(workflowService.stencils(StateTypeScope.PIPELINE_STENCILS))
        .thenReturn(newHashMap(of(StateTypeScope.PIPELINE_STENCILS, newArrayList(StateType.ENV_STATE))));

    RestResponse<Map<String, Object>> actual =
        resources.client()
            .target("/catalogs?catalogType=" + CatalogNames.PIPELINE_STENCILS + "&appId=" + APP_ID)
            .request()
            .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual)
        .isNotNull()
        .extracting(RestResponse::getResource)
        .hasSize(1)
        .extracting(o -> ((Map<String, Object>) o).get(CatalogNames.PIPELINE_STENCILS))
        .isNotNull();
    verify(workflowService).stencils(StateTypeScope.PIPELINE_STENCILS);
    verify(environmentService).listForEnum(APP_ID);
  }
}
