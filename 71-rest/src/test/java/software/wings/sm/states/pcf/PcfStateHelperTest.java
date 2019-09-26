package software.wings.sm.states.pcf;

import static io.harness.pcf.model.PcfConstants.MANIFEST_YML;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.APP_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.ServiceElement;
import software.wings.beans.FeatureName;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.PcfServiceSpecification;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.FeatureFlagService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.sm.ExecutionContext;

public class PcfStateHelperTest extends WingsBaseTest {
  @Mock private ApplicationManifestService applicationManifestService;
  @Mock private FeatureFlagService featureFlagService;
  @Mock private ServiceResourceService serviceResourceService;
  @InjectMocks @Inject private PcfStateHelper pcfStateHelper;
  @Mock private ExecutionContext context;

  @Before
  public void setup() throws IllegalAccessException {
    ApplicationManifest manifest = ApplicationManifest.builder()
                                       .serviceId(SERVICE_ID)
                                       .kind(AppManifestKind.K8S_MANIFEST)
                                       .storeType(StoreType.Local)
                                       .build();
    manifest.setUuid("1234");

    when(context.getAppId()).thenReturn(APP_ID);
    when(applicationManifestService.getByServiceId(anyString(), anyString(), any())).thenReturn(manifest);
    when(applicationManifestService.getManifestFileByFileName(anyString(), anyString()))
        .thenReturn(
            ManifestFile.builder().fileName(MANIFEST_YML).fileContent(PcfSetupStateTest.MANIFEST_YAML_CONTENT).build());
  }

  @Test
  @Category(UnitTests.class)
  public void testFetchManifestYmlString() throws Exception {
    when(featureFlagService.isEnabled(FeatureName.PCF_MANIFEST_REDESIGN, ACCOUNT_ID))
        .thenReturn(true)
        .thenReturn(false);
    String yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);

    when(serviceResourceService.getPcfServiceSpecification(APP_ID, SERVICE_ID))
        .thenReturn(PcfServiceSpecification.builder()
                        .serviceId(SERVICE_ID)
                        .manifestYaml(PcfSetupStateTest.MANIFEST_YAML_CONTENT)
                        .build());
    yaml = pcfStateHelper.fetchManifestYmlString(context,
        anApplication().uuid(APP_ID).appId(APP_ID).accountId(ACCOUNT_ID).name(APP_NAME).build(),
        ServiceElement.builder().uuid(SERVICE_ID).build());
    assertThat(yaml).isNotNull();
    assertThat(yaml).isEqualTo(PcfSetupStateTest.MANIFEST_YAML_CONTENT);
  }
}
