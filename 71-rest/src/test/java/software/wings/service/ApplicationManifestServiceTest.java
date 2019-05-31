package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;
import static software.wings.beans.appmanifest.AppManifestKind.K8S_MANIFEST;
import static software.wings.beans.appmanifest.AppManifestKind.VALUES;
import static software.wings.beans.appmanifest.StoreType.Local;
import static software.wings.beans.appmanifest.StoreType.Remote;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;

import com.google.inject.Inject;

import io.harness.category.element.UnitTests;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.RealMongo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.api.DeploymentType;
import software.wings.beans.GitFileConfig;
import software.wings.beans.Service;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ApplicationManifest.ApplicationManifestKeys;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.ApplicationManifestServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ApplicationManifestService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.yaml.YamlPushService;

import java.util.List;

public class ApplicationManifestServiceTest extends WingsBaseTest {
  private static final String GIT_CONNECTOR_ID = "gitConnectorId";
  private static final String BRANCH = "branch";
  private static final String FILE_PATH = "filePath";
  private static final String FILE_NAME = "fileName";
  private static final String FILE_CONTENT = "fileContent";

  @Mock private AppService appService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private YamlPushService yamlPushService;

  @Inject private WingsPersistence wingsPersistence;

  @Inject @InjectMocks ApplicationManifestService applicationManifestService;
  @Inject ApplicationManifestServiceImpl applicationManifestServiceImpl;

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    applicationManifest.setAppId(APP_ID);
    manifestFile.setAppId(APP_ID);
  }

  private static ApplicationManifest applicationManifest =
      ApplicationManifest.builder().serviceId(SERVICE_ID).storeType(Local).kind(AppManifestKind.K8S_MANIFEST).build();

  private static ManifestFile manifestFile =
      ManifestFile.builder().fileName("deploy.yaml").fileContent("deployment spec").build();

  @Test
  @Category(UnitTests.class)
  public void createShouldFailIfServiceDoesNotExist() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(false);

    try {
      applicationManifestService.create(applicationManifest);
    } catch (InvalidRequestException e) {
      assertThat(e.getParams().get("message")).isEqualTo("Service doesn't exist");
    }
  }

  @Test
  @Category(UnitTests.class)
  public void createTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    assertThat(savedManifest.getUuid()).isNotEmpty();
    assertThat(savedManifest.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(savedManifest.getStoreType()).isEqualTo(Local);

    ApplicationManifest manifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                       .filter(ApplicationManifest.APP_ID_KEY, APP_ID)
                                       .filter(ApplicationManifestKeys.serviceId, SERVICE_ID)
                                       .get();

    assertThat(manifest).isEqualTo(savedManifest);
  }

  @Test
  @Category(UnitTests.class)
  public void updateTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    // savedManifest.setManifestFiles(asList(manifestFile));

    applicationManifestService.update(savedManifest);

    ApplicationManifest manifest = wingsPersistence.createQuery(ApplicationManifest.class)
                                       .filter(ApplicationManifest.APP_ID_KEY, APP_ID)
                                       .filter(ApplicationManifestKeys.serviceId, SERVICE_ID)
                                       .get();

    // assertThat(manifest.getManifestFiles()).isEqualTo(asList(manifestFile));
  }

  @Test
  @Category(UnitTests.class)
  public void getTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    ApplicationManifest savedManifest = applicationManifestService.create(applicationManifest);

    ApplicationManifest manifest = applicationManifestService.getK8sManifestByServiceId(APP_ID, SERVICE_ID);

    assertThat(manifest).isEqualTo(savedManifest);
  }

  @Test
  @Category(UnitTests.class)
  public void deleteTest() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);
    applicationManifestService.create(applicationManifest);
    manifestFile.setApplicationManifestId(applicationManifest.getUuid());

    ManifestFile savedmManifestFile = applicationManifestService.createManifestFileByServiceId(
        ApplicationManifestServiceTest.manifestFile, SERVICE_ID);

    ManifestFile manifestFileById = applicationManifestService.getManifestFileById(APP_ID, manifestFile.getUuid());
    assertThat(savedmManifestFile).isEqualTo(manifestFileById);

    applicationManifestService.deleteManifestFileById(APP_ID, savedmManifestFile.getUuid());
    manifestFileById = applicationManifestService.getManifestFileById(APP_ID, manifestFile.getUuid());
    assertNull(manifestFileById);
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  @RealMongo
  public void testDuplicateManifestFileNames() {
    when(serviceResourceService.exist(anyString(), anyString())).thenReturn(true);

    wingsPersistence.ensureIndex(ManifestFile.class);
    applicationManifestService.create(applicationManifest);

    ManifestFile manifestFileWithSameName =
        ManifestFile.builder().fileName("deploy.yaml").fileContent("duplicate deployment spec").build();
    manifestFileWithSameName.setAppId(APP_ID);

    ManifestFile savedmManifestFile =
        applicationManifestService.createManifestFileByServiceId(manifestFile, SERVICE_ID);
    assertNotNull(savedmManifestFile);
    applicationManifestService.createManifestFileByServiceId(manifestFileWithSameName, SERVICE_ID);
  }

  @Test
  @Category(UnitTests.class)
  public void testCreateAppManifestForService() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);

    ApplicationManifest savedApplicationManifest = applicationManifestService.create(applicationManifest);
    assertThat(savedApplicationManifest.getUuid()).isNotNull();
    assertThat(savedApplicationManifest.getGitFileConfig()).isNull();
    assertThat(savedApplicationManifest.getEnvId()).isNull();
    assertThat(savedApplicationManifest.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(savedApplicationManifest.getStoreType()).isEqualTo(Local);
    assertThat(savedApplicationManifest.getKind()).isEqualTo(K8S_MANIFEST);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpdateAppManifestForService() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    wingsPersistence.save(applicationManifest);

    GitFileConfig gitFileConfig = GitFileConfig.builder()
                                      .connectorId(GIT_CONNECTOR_ID)
                                      .useBranch(true)
                                      .branch(BRANCH)
                                      .filePath(FILE_PATH)
                                      .build();
    applicationManifest.setStoreType(Remote);
    applicationManifest.setGitFileConfig(gitFileConfig);

    ApplicationManifest savedApplicationManifest = applicationManifestService.update(applicationManifest);
    assertThat(savedApplicationManifest.getUuid()).isNotNull();
    assertThat(savedApplicationManifest.getEnvId()).isNull();
    assertThat(savedApplicationManifest.getServiceId()).isEqualTo(SERVICE_ID);
    assertThat(savedApplicationManifest.getStoreType()).isEqualTo(Remote);
    assertThat(savedApplicationManifest.getKind()).isEqualTo(K8S_MANIFEST);
    compareGitFileConfig(gitFileConfig, applicationManifest.getGitFileConfig());
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testCreateInvalidRemoteAppManifest() {
    GitFileConfig gitFileConfig = GitFileConfig.builder().useBranch(true).branch(BRANCH).filePath(FILE_PATH).build();

    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .storeType(Remote)
                                                  .kind(K8S_MANIFEST)
                                                  .serviceId(SERVICE_ID)
                                                  .gitFileConfig(gitFileConfig)
                                                  .build();

    applicationManifestService.create(applicationManifest);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testCreateInvalidLocalAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder()
                                                  .storeType(Local)
                                                  .kind(K8S_MANIFEST)
                                                  .serviceId(SERVICE_ID)
                                                  .gitFileConfig(GitFileConfig.builder().build())
                                                  .build();

    applicationManifestService.create(applicationManifest);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testCreateInvalidAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).build();

    applicationManifestService.create(applicationManifest);
  }

  private void compareGitFileConfig(GitFileConfig gitFileConfig, GitFileConfig savedGitFileConfig) {
    assertThat(savedGitFileConfig.getFilePath()).isEqualTo(gitFileConfig.getFilePath());
    assertThat(savedGitFileConfig.getConnectorId()).isEqualTo(gitFileConfig.getConnectorId());
    assertThat(savedGitFileConfig.isUseBranch()).isEqualTo(gitFileConfig.isUseBranch());
    assertThat(savedGitFileConfig.getBranch()).isEqualTo(gitFileConfig.getBranch());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpdateAppManifestKind() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    wingsPersistence.save(applicationManifest);

    applicationManifest.setKind(AppManifestKind.VALUES);
    ApplicationManifest savedApplicationManifest = applicationManifestService.update(applicationManifest);
    assertThat(savedApplicationManifest.getKind()).isEqualTo(K8S_MANIFEST);
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertApplicationManifestFileForCreate() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    wingsPersistence.save(applicationManifest);

    ManifestFile manifestFile = ManifestFile.builder().fileContent(FILE_CONTENT).fileName(FILE_NAME).build();
    manifestFile.setAppId(APP_ID);

    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
    assertThat(manifestFile.getFileName()).isEqualTo(FILE_NAME);
    assertThat(manifestFile.getFileContent()).isEqualTo(FILE_CONTENT);
    assertThat(manifestFile.getApplicationManifestId()).isEqualTo(applicationManifest.getUuid());
  }

  @Test
  @Category(UnitTests.class)
  public void testUpsertApplicationManifestFileForUpdate() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    wingsPersistence.save(applicationManifest);

    ManifestFile manifestFile = ManifestFile.builder().fileContent(FILE_CONTENT).fileName(FILE_NAME).build();
    manifestFile.setAppId(APP_ID);
    wingsPersistence.save(manifestFile);

    manifestFile.setFileName("updated" + FILE_NAME);
    manifestFile.setFileContent("updated" + FILE_CONTENT);
    manifestFile.setApplicationManifestId("randomId");

    ManifestFile savedManifestFile =
        applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
    assertThat(savedManifestFile.getFileName()).isEqualTo("updated" + FILE_NAME);
    assertThat(savedManifestFile.getFileContent()).isEqualTo("updated" + FILE_CONTENT);
    assertThat(savedManifestFile.getApplicationManifestId()).isEqualTo(applicationManifest.getUuid());
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName() {
    validateManifestFileName("  ");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName1() {
    validateManifestFileName(" / ");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName2() {
    validateManifestFileName("a//c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName3() {
    validateManifestFileName("a/ /c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName4() {
    validateManifestFileName("a/b /c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName5() {
    validateManifestFileName("a/b/ c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName6() {
    validateManifestFileName("a/b/c ");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName7() {
    validateManifestFileName("a/ b/c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName8() {
    validateManifestFileName(" a/b/c");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidManifestFileName9() {
    validateManifestFileName("a /b/c");
  }

  private void validateManifestFileName(String fileName) {
    ManifestFile manifestFile = ManifestFile.builder().fileContent(FILE_CONTENT).fileName(fileName).build();
    manifestFile.setAppId(APP_ID);

    applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateFileNamePrefixForDirectory() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    wingsPersistence.save(applicationManifest);

    ManifestFile manifestFile1 = getManifestFileWithName("a/b/c");
    ManifestFile manifestFile2 = getManifestFileWithName("a/b");

    applicationManifestService.upsertApplicationManifestFile(manifestFile1, applicationManifest, true);
    applicationManifestService.upsertApplicationManifestFile(manifestFile2, applicationManifest, true);
  }

  private ManifestFile getManifestFileWithName(String fileName) {
    ManifestFile manifestFile = ManifestFile.builder().fileContent(FILE_CONTENT).fileName(fileName).build();
    manifestFile.setAppId(APP_ID);

    return manifestFile;
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteAppManifest() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile1 = getManifestFileWithName("a");
    ManifestFile manifestFile2 = getManifestFileWithName("b");

    applicationManifestService.upsertApplicationManifestFile(manifestFile1, applicationManifest, true);
    applicationManifestService.upsertApplicationManifestFile(manifestFile2, applicationManifest, true);

    applicationManifestService.deleteAppManifest(APP_ID, applicationManifest.getUuid());

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getK8sManifestByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteAppManifestMultipleTimes() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile1 = getManifestFileWithName("a");
    ManifestFile manifestFile2 = getManifestFileWithName("b");

    applicationManifestService.upsertApplicationManifestFile(manifestFile1, applicationManifest, true);
    applicationManifestService.upsertApplicationManifestFile(manifestFile2, applicationManifest, true);

    applicationManifestService.deleteAppManifest(APP_ID, applicationManifest.getUuid());
    applicationManifestService.deleteAppManifest(APP_ID, applicationManifest.getUuid());

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getK8sManifestByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteManifestFileForService() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile = getManifestFileWithName("a");

    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
    applicationManifestService.deleteManifestFileById(APP_ID, manifestFile.getUuid());

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getK8sManifestByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNotNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteManifestFileForEnvironment() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(VALUES).envId(ENV_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile = getManifestFileWithName("a");
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);

    applicationManifestService.deleteManifestFile(APP_ID, manifestFile);

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getByEnvId(APP_ID, ENV_ID, VALUES);
    assertThat(applicationManifest).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testDeleteManifestFileForEnvironmentMultipleTimes() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(VALUES).envId(ENV_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile = getManifestFileWithName("a");
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);

    applicationManifestService.deleteManifestFile(APP_ID, manifestFile);
    applicationManifestService.deleteManifestFile(APP_ID, manifestFile);

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getByEnvId(APP_ID, ENV_ID, VALUES);
    assertThat(applicationManifest).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testPruneByService() {
    ApplicationManifest applicationManifest =
        ApplicationManifest.builder().storeType(Local).kind(K8S_MANIFEST).serviceId(SERVICE_ID).build();
    applicationManifest.setAppId(APP_ID);
    applicationManifest = applicationManifestService.create(applicationManifest);

    ManifestFile manifestFile1 = getManifestFileWithName("a");
    ManifestFile manifestFile2 = getManifestFileWithName("b");

    applicationManifestService.upsertApplicationManifestFile(manifestFile1, applicationManifest, true);
    applicationManifestService.upsertApplicationManifestFile(manifestFile2, applicationManifest, true);

    applicationManifestService.pruneByService(APP_ID, SERVICE_ID);

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, applicationManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    applicationManifest = applicationManifestService.getK8sManifestByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
  @Category(UnitTests.class)
  public void testPruneByEnvironment() {
    ApplicationManifest envAppManifest =
        ApplicationManifest.builder().storeType(Local).kind(VALUES).envId(ENV_ID).build();
    envAppManifest.setAppId(APP_ID);
    envAppManifest = applicationManifestService.create(envAppManifest);
    ManifestFile manifestFile1 = getManifestFileWithName("a");
    applicationManifestService.upsertApplicationManifestFile(manifestFile1, envAppManifest, true);

    ApplicationManifest envServiceAppManifest =
        ApplicationManifest.builder().storeType(Local).kind(VALUES).envId(ENV_ID).serviceId(SERVICE_ID).build();
    envServiceAppManifest.setAppId(APP_ID);
    envServiceAppManifest = applicationManifestService.create(envServiceAppManifest);
    ManifestFile manifestFile2 = getManifestFileWithName("a");
    applicationManifestService.upsertApplicationManifestFile(manifestFile2, envServiceAppManifest, true);

    applicationManifestService.pruneByEnvironment(APP_ID, ENV_ID);

    List<ManifestFile> manifestFiles =
        applicationManifestService.getManifestFilesByAppManifestId(APP_ID, envAppManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    manifestFiles = applicationManifestService.getManifestFilesByAppManifestId(APP_ID, envServiceAppManifest.getUuid());
    assertThat(manifestFiles).isEmpty();
    List<ApplicationManifest> applicationManifests =
        applicationManifestService.getAllByEnvIdAndKind(APP_ID, ENV_ID, VALUES);
    assertThat(applicationManifests).isEmpty();
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateManifestFileName() {
    upsertManifestFile("abc/def", "abc/pqr");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName1() {
    upsertManifestFile("abc/def", "abc");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName2() {
    upsertManifestFile("abc/def/ghi", "abc");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName3() {
    upsertManifestFile("abc/def/ghi", "abc/def");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName4() {
    upsertManifestFile("abc", "abc/def");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName5() {
    upsertManifestFile("abc/def", "abc/def/ghi");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName6() {
    upsertManifestFile("abc/def", "abc/def/ghi/klm");
  }

  @Test(expected = WingsException.class)
  @Category(UnitTests.class)
  @RealMongo
  public void testDuplicateManifestFileName() {
    wingsPersistence.ensureIndex(ManifestFile.class);
    upsertManifestFile("abc/def", "abc/def");
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testValidateManifestFileName8() {
    upsertManifestFile("abc/def", "abc/ghi");
    upsertManifestFile("abc/jkl", "abc/mno");
  }

  @Test
  @Category(UnitTests.class)
  public void testEditManifestFileContent() {
    ApplicationManifest appManifest = createAppManifest();

    ManifestFile manifestFile = getManifestFileWithName("abc/values.yaml");
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    manifestFile.setFileContent("file-content-abc");
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    ManifestFile manifestFileById =
        applicationManifestService.getManifestFileById(manifestFile.getAppId(), manifestFile.getUuid());

    assertThat(manifestFileById.getFileName()).isEqualTo("abc/values.yaml");
    assertThat(manifestFileById.getFileContent()).isEqualTo("file-content-abc");
  }

  @Test
  @Category(UnitTests.class)
  public void testEditManifestFileName() {
    ApplicationManifest appManifest = createAppManifest();

    ManifestFile manifestFile = getManifestFileWithName("abc/values.yaml");
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    manifestFile.setFileName("xyz");
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    ManifestFile manifestFileById =
        applicationManifestService.getManifestFileById(manifestFile.getAppId(), manifestFile.getUuid());

    assertThat(manifestFileById.getFileName()).isEqualTo("xyz");
    assertThat(manifestFileById.getFileContent()).isEqualTo(FILE_CONTENT);
  }

  @Test(expected = InvalidRequestException.class)
  @Category(UnitTests.class)
  public void testMoveManifestFileToExistingDirectory() {
    ApplicationManifest appManifest = createAppManifest();

    ManifestFile manifestFile = getManifestFileWithName("abc/file1");
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    manifestFile = getManifestFileWithName("xyz/file2");
    manifestFile = applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    manifestFile.setFileName("abc");
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);
  }

  private ApplicationManifest createAppManifest() {
    ApplicationManifest envAppManifest =
        ApplicationManifest.builder().storeType(Local).kind(VALUES).serviceId(SERVICE_ID).build();
    envAppManifest.setAppId(APP_ID);
    return applicationManifestService.create(envAppManifest);
  }

  private void upsertManifestFile(String fileName1, String fileName2) {
    ApplicationManifest appManifest = createAppManifest();

    ManifestFile manifestFile = getManifestFileWithName(fileName1);
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);

    manifestFile = getManifestFileWithName(fileName2);
    applicationManifestService.upsertApplicationManifestFile(manifestFile, appManifest, true);
  }

  @Test
  @Category(UnitTests.class)
  public void testRemoveNamespace() {
    applicationManifestServiceImpl.removeNamespace(null);

    ManifestFile manifestFile = ManifestFile.builder().build();
    applicationManifestServiceImpl.removeNamespace(manifestFile);

    String fileContent = null;
    manifestFile.setFileContent(fileContent);
    applicationManifestServiceImpl.removeNamespace(manifestFile);
    assertTrue(manifestFile.getFileContent() == null);

    fileContent = "";
    manifestFile.setFileContent(fileContent);
    applicationManifestServiceImpl.removeNamespace(manifestFile);
    assertTrue(manifestFile.getFileContent().equals(""));

    fileContent = "{{- if .Values.env.config}}\n"
        + "apiVersion: v1\n"
        + "kind: ConfigMap\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "data:\n"
        + "{{.Values.env.config | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.env.secrets}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "stringData:\n"
        + "{{.Values.env.secrets | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.createImagePullSecret}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-dockercfg\n"
        + "  annotations:\n"
        + "    harness.io/skip-versioning: true\n"
        + "data:\n"
        + "  .dockercfg: {{.Values.dockercfg}}\n"
        + "type: kubernetes.io/dockercfg\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "apiVersion: apps/v1\n"
        + "kind: Deployment\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-deployment\n"
        + "spec:\n"
        + "  replicas: {{int .Values.replicas}}\n"
        + "  selector:\n"
        + "    matchLabels:\n"
        + "      app: {{.Values.name}}\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      labels:\n"
        + "        app: {{.Values.name}}\n"
        + "    spec:\n"
        + "      {{- if .Values.createImagePullSecret}}\n"
        + "      imagePullSecrets:\n"
        + "      - name: {{.Values.name}}-dockercfg\n"
        + "      {{- end}}\n"
        + "      containers:\n"
        + "      - name: {{.Values.name}}\n"
        + "        image: {{.Values.image}}\n"
        + "        {{- if or .Values.env.config .Values.env.secrets}}\n"
        + "        envFrom:\n"
        + "        {{- if .Values.env.config}}\n"
        + "        - configMapRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- if .Values.env.secrets}}\n"
        + "        - secretRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- end}}";
    manifestFile.setFileContent(fileContent);
    applicationManifestServiceImpl.removeNamespace(manifestFile);
    assertTrue(manifestFile.getFileContent().equals(fileContent));

    fileContent = "{{- if .Values.env.config}}\n"
        + "apiVersion: v1\n"
        + "kind: ConfigMap\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "data:\n"
        + "{{.Values.env.config | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.env.secrets}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "stringData:\n"
        + "{{.Values.env.secrets | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.createImagePullSecret}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-dockercfg\n"
        + "  annotations:\n"
        + "    harness.io/skip-versioning: true\n"
        + "  namespace: abc\n"
        + "data:\n"
        + "  .dockercfg: {{.Values.dockercfg}}\n"
        + "type: kubernetes.io/dockercfg\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "apiVersion: apps/v1\n"
        + "kind: Deployment\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-deployment\n"
        + "spec:\n"
        + "  replicas: {{int .Values.replicas}}\n"
        + "  selector:\n"
        + "    matchLabels:\n"
        + "      app: {{.Values.name}}\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      labels:\n"
        + "        app: {{.Values.name}}\n"
        + "    spec:\n"
        + "      - name: {{.Values.name}}\n"
        + "        image: {{.Values.image}}\n"
        + "        {{- if or .Values.env.config .Values.env.secrets}}\n"
        + "        envFrom:\n"
        + "        {{- if .Values.env.config}}\n"
        + "        - configMapRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- if .Values.env.secrets}}\n"
        + "        - secretRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- end}}";

    String expectedFileContent = "{{- if .Values.env.config}}\n"
        + "apiVersion: v1\n"
        + "kind: ConfigMap\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "data:\n"
        + "{{.Values.env.config | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.env.secrets}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}\n"
        + "stringData:\n"
        + "{{.Values.env.secrets | toYaml | indent 2}}\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "{{- if .Values.createImagePullSecret}}\n"
        + "apiVersion: v1\n"
        + "kind: Secret\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-dockercfg\n"
        + "  annotations:\n"
        + "    harness.io/skip-versioning: true\n"
        + "data:\n"
        + "  .dockercfg: {{.Values.dockercfg}}\n"
        + "type: kubernetes.io/dockercfg\n"
        + "---\n"
        + "{{- end}}\n"
        + "\n"
        + "apiVersion: apps/v1\n"
        + "kind: Deployment\n"
        + "metadata:\n"
        + "  name: {{.Values.name}}-deployment\n"
        + "spec:\n"
        + "  replicas: {{int .Values.replicas}}\n"
        + "  selector:\n"
        + "    matchLabels:\n"
        + "      app: {{.Values.name}}\n"
        + "  template:\n"
        + "    metadata:\n"
        + "      labels:\n"
        + "        app: {{.Values.name}}\n"
        + "    spec:\n"
        + "      - name: {{.Values.name}}\n"
        + "        image: {{.Values.image}}\n"
        + "        {{- if or .Values.env.config .Values.env.secrets}}\n"
        + "        envFrom:\n"
        + "        {{- if .Values.env.config}}\n"
        + "        - configMapRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- if .Values.env.secrets}}\n"
        + "        - secretRef:\n"
        + "            name: {{.Values.name}}\n"
        + "        {{- end}}\n"
        + "        {{- end}}";

    manifestFile.setFileContent(fileContent);
    applicationManifestServiceImpl.removeNamespace(manifestFile);
    assertTrue(manifestFile.getFileContent().equals(expectedFileContent));
  }

  @Test
  @Category(UnitTests.class)
  public void testDoFileSizeValidation() {
    String content = "abcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabcabc";
    ManifestFile manifestFile = ManifestFile.builder().fileContent(content).build();
    applicationManifestServiceImpl.doFileSizeValidation(manifestFile);
  }

  @Test
  @Category(UnitTests.class)
  public void testValidateLocalAppManifest() {
    ApplicationManifest applicationManifest = ApplicationManifest.builder().storeType(StoreType.Local).build();
    applicationManifestServiceImpl.validateLocalAppManifest(applicationManifest);

    applicationManifest.setServiceId("testservice");
    Service service = Service.builder().build();
    when(serviceResourceService.get(any(), any())).thenReturn(service);
    applicationManifestServiceImpl.validateLocalAppManifest(applicationManifest);

    service.setDeploymentType(DeploymentType.KUBERNETES);
    applicationManifestServiceImpl.validateLocalAppManifest(applicationManifest);

    applicationManifest.setKind(AppManifestKind.VALUES);
    applicationManifestServiceImpl.validateLocalAppManifest(applicationManifest);

    service.setDeploymentType(DeploymentType.HELM);
    applicationManifest.setKind(AppManifestKind.K8S_MANIFEST);
    assertThatExceptionOfType(InvalidRequestException.class)
        .isThrownBy(() -> applicationManifestServiceImpl.validateLocalAppManifest(applicationManifest));
  }
}
