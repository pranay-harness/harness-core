package software.wings.resources;

import static com.google.common.base.CaseFormat.UPPER_CAMEL;
import static com.google.common.base.CaseFormat.UPPER_UNDERSCORE;
import static com.google.common.collect.Lists.newArrayList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static software.wings.beans.CatalogNames.EXECUTION_TYPE;
import static software.wings.beans.SettingAttribute.Builder.aSettingAttribute;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

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
import software.wings.service.intfc.JenkinsBuildService;
import software.wings.service.intfc.SettingsService;
import software.wings.utils.ResourceTestRule;

import java.io.IOException;
import java.util.ArrayList;
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
  private static final JenkinsBuildService jenkinsBuildService = mock(JenkinsBuildService.class);
  private static final SettingsService settingsService = mock(SettingsService.class);

  /**
   * The constant resources.
   */
  @ClassRule
  public static final ResourceTestRule resources =
      ResourceTestRule.builder()
          .addResource(new CatalogResource(catalogService, jenkinsBuildService, settingsService))
          .build();

  /**
   * The Verifier.
   */
  @Rule
  public Verifier verifier = new Verifier() {
    @Override
    protected void verify() throws Throwable {
      verifyNoMoreInteractions(catalogService, jenkinsBuildService, settingsService);
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
  }

  /**
   * Tear down.
   */
  @After
  public void tearDown() {
    reset(catalogService, jenkinsBuildService, settingsService);
  }

  /**
   * Should list catalogs.
   */
  @Test
  public void shouldListCatalogs() {
    when(catalogService.getCatalogItems(anyString())).thenReturn(new ArrayList<>());

    RestResponse<Map<String, Object>> actual =
        resources.client()
            .target("/catalogs?catalogType=EXECUTION_TYPE&catalogType=CARD_VIEW_SORT_BY")
            .request()
            .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual).isNotNull();
    assertThat(actual.getResource()).isNotNull().hasSize(2).containsKeys(EXECUTION_TYPE, "CARD_VIEW_SORT_BY");
  }

  private Object[][] catalogNames() {
    return new Object[][] {{UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.JENKINS_BUILD)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.JENKINS_CONFIG)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.CONNECTION_ATTRIBUTES)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.BASTION_HOST_ATTRIBUTES)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.EXECUTION_TYPE)},
        {UPPER_UNDERSCORE.to(UPPER_CAMEL, CatalogNames.ENVIRONMENT_TYPE)}};
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
    RestResponse<Map<String, Object>> actual =
        resources.client()
            .target("/catalogs?catalogType=" + catalogName + "&appId=" + APP_ID + "&serviceId=" + SERVICE_ID)
            .request()
            .get(new GenericType<RestResponse<Map<String, Object>>>() {});

    assertThat(actual)
        .isNotNull()
        .extracting(RestResponse::getResource)
        .hasSize(1)
        .extracting(o -> ((Map<String, Object>) o).get(catalogName))
        .isNotNull();
  }
}
