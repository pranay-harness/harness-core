package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlPushService;

public class ApplicationManifestServiceTest extends WingsBaseTest {
  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private YamlPushService yamlPushService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks ApplicationManifestService applicationManifestService;

  @Before
  public void setUp() throws Exception {
    applicationManifest.setAppId(APP_ID);
    manifestFile.setAppId(APP_ID);
  }

  private static ApplicationManifest applicationManifest =
      ApplicationManifest.builder().serviceId(SERVICE_ID).storeType(StoreType.Local).build();

  private static ManifestFile manifestFile =
      ManifestFile.builder().fileName("deploy.yaml").fileContent("deployment spec").build();

  @Test
  public void createShouldFailIfServiceDoesNotExist() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(false);

    try {
      applicationManifestService.create(applicationManifest);
    } catch (InvalidRequestException e) {
      assertThat(e.getParams().get("message")).isEqualTo("Service doesn't exist");
    }
  }

  @Test
  public void createTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    assertThat(savedManifest.getUuid()).isNotEmpty();
    assertThat(savedManifest.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(savedManifest.getStoreType()).isEqualTo(StoreType.Local);

    ApplicationManifest manifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                       .filter(ApplicationManifest.APP_ID_KEY, APP_ID)
                                       .filter(ApplicationManifest.SERVICE_ID_KEY, SERVICE_ID)
                                       .get();

    assertThat(manifest).isEqualTo(savedManifest);
  }

  @Test
  public void updateTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    // savedManifest.setManifestFiles(asList(manifestFile));

    applicationManifestService.update(savedManifest);

    ApplicationManifest manifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                       .filter(ApplicationManifest.APP_ID_KEY, APP_ID)
                                       .filter(ApplicationManifest.SERVICE_ID_KEY, SERVICE_ID)
                                       .get();

    // assertThat(manifest.getManifestFiles()).isEqualTo(asList(manifestFile));
  }

  @Test
  public void getTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    ApplicationManifest manifest = applicationManifestService.get(APP_ID, SERVICE_ID);

    assertThat(manifest).isEqualTo(savedManifest);
  }

  @Test
  public void deleteTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    ManifestFile savedmManifestFile =
        applicationManifestService.createManifestFile(ApplicationManifestServiceTest.manifestFile, SERVICE_ID);

    ManifestFile manifestFileById = applicationManifestService.getManifestFileById(APP_ID, manifestFile.getUuid());
    assertThat(savedmManifestFile).isEqualTo(manifestFileById);

    applicationManifestService.deleteManifestFileById(APP_ID, savedmManifestFile.getUuid());
    manifestFileById = applicationManifestService.getManifestFileById(APP_ID, manifestFile.getUuid());
    assertNull(manifestFileById);
  }
}
