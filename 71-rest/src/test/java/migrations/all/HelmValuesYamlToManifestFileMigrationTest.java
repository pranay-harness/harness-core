package migrations.all;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.common.Constants.VALUES_YAML_KEY;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.ENV_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_NAME;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.Account;
import software.wings.beans.Application;
import software.wings.beans.Environment;
import software.wings.beans.Environment.Builder;
import software.wings.beans.Service;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.HashMap;
import java.util.Map;

public class HelmValuesYamlToManifestFileMigrationTest extends WingsBaseTest {
  private static final String ENV_HELM_VALUE = "Environment-helmValue";
  private static final String ENV_SERVICE_HELM_VALUE = "EnvironmentService-helmValue";
  private static final String SERVICE_HELM_VALUE = "Service-helmValue";

  @Mock private YamlPushService yamlPushService;
  @Mock private AppService appService;
  @Mock private AccountService accountService;
  @Mock private Application application;
  @Mock private Account account;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;
  @Inject private WingsPersistence wingsPersistence;
  @Inject private ApplicationManifestService applicationManifestService;

  @InjectMocks @Inject private HelmValuesYamlToManifestFileMigration helmValuesYamlToManifestFileMigration;

  private Environment environment;
  private Service service;

  @Before
  public void setupMocks() {
    when(appService.get(APP_ID)).thenReturn(application);
    when(accountService.get(anyString())).thenReturn(account);
    wingsPersistence.save(anApplication().withUuid(APP_ID).withAccountId(ACCOUNT_ID).build());
    when(serviceTemplateService.get(anyString(), anyString()))
        .thenReturn(ServiceTemplate.Builder.aServiceTemplate().withServiceId(SERVICE_ID).build());

    environment = Builder.anEnvironment()
                      .withUuid(ENV_ID)
                      .withAppId(APP_ID)
                      .withName(ENV_NAME)
                      .withHelmValueYaml(ENV_HELM_VALUE)
                      .build();
    service =
        Service.builder().uuid(SERVICE_ID).appId(APP_ID).name(SERVICE_NAME).helmValueYaml(SERVICE_HELM_VALUE).build();
  }

  @Test
  @Category(UnitTests.class)
  public void testMigrationOfEnvironmentOverride() {
    wingsPersistence.save(environment);
    helmValuesYamlToManifestFileMigration.migrate();

    ApplicationManifest appManifest = applicationManifestService.getByEnvId(APP_ID, ENV_ID, AppManifestKind.VALUES);
    ManifestFile manifestFile =
        applicationManifestService.getManifestFileByFileName(appManifest.getUuid(), VALUES_YAML_KEY);

    assertThat(appManifest.getAppId()).isEqualTo(APP_ID);
    assertThat(appManifest.getEnvId()).isEqualTo(ENV_ID);
    assertThat(appManifest.getServiceId()).isNull();
    assertThat(appManifest.getStoreType()).isEqualTo(StoreType.Local);
    assertThat(appManifest.getKind()).isEqualTo(AppManifestKind.VALUES);
    assertThat(appManifest.getGitFileConfig()).isNull();

    assertThat(manifestFile.getFileContent()).isEqualTo(ENV_HELM_VALUE);
    assertThat(manifestFile.getAppId()).isEqualTo(APP_ID);
    assertThat(manifestFile.getApplicationManifestId()).isEqualTo(appManifest.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void testMigrationOfEnvironmentServiceOverride() {
    Map<String, String> helmServiceTemplateValueOverride = new HashMap<>();
    helmServiceTemplateValueOverride.put(SERVICE_ID, ENV_SERVICE_HELM_VALUE);
    environment.setHelmValueYamlByServiceTemplateId(helmServiceTemplateValueOverride);
    wingsPersistence.save(environment);
    wingsPersistence.save(service);
    helmValuesYamlToManifestFileMigration.migrate();

    ApplicationManifest appManifest =
        applicationManifestService.getByEnvAndServiceId(APP_ID, ENV_ID, SERVICE_ID, AppManifestKind.VALUES);
    ManifestFile manifestFile =
        applicationManifestService.getManifestFileByFileName(appManifest.getUuid(), VALUES_YAML_KEY);

    assertThat(appManifest.getAppId()).isEqualTo(APP_ID);
    assertThat(appManifest.getEnvId()).isEqualTo(ENV_ID);
    assertThat(appManifest.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(appManifest.getStoreType()).isEqualTo(StoreType.Local);
    assertThat(appManifest.getKind()).isEqualTo(AppManifestKind.VALUES);
    assertThat(appManifest.getGitFileConfig()).isNull();

    assertThat(manifestFile.getFileContent()).isEqualTo(ENV_SERVICE_HELM_VALUE);
    assertThat(manifestFile.getAppId()).isEqualTo(APP_ID);
    assertThat(manifestFile.getApplicationManifestId()).isEqualTo(appManifest.getUuid());
  }
}
