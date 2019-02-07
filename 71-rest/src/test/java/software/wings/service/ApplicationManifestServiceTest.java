package software.wings.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.rule.RealMongo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.WingsBaseTest;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.AppManifestKind;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.dl.WingsPersistence;
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

  @Rule public ExpectedException thrown = ExpectedException.none();

  @Before
  public void setUp() throws Exception {
    applicationManifest.setAppId(APP_ID);
    manifestFile.setAppId(APP_ID);
  }

  private static ApplicationManifest applicationManifest =
      ApplicationManifest.builder().serviceId(SERVICE_ID).storeType(Local).kind(AppManifestKind.VALUES).build();

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
    assertThat(savedManifest.getStoreType()).isEqualTo(Local);

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

    ApplicationManifest manifest = applicationManifestService.getByServiceId(APP_ID, SERVICE_ID);

    assertThat(manifest).isEqualTo(savedManifest);
  }

  @Test
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
  public void testValidManifestFileName() {
    validateManifestFileName("  ");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName1() {
    validateManifestFileName(" / ");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName2() {
    validateManifestFileName("a//c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName3() {
    validateManifestFileName("a/ /c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName4() {
    validateManifestFileName("a/b /c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName5() {
    validateManifestFileName("a/b/ c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName6() {
    validateManifestFileName("a/b/c ");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName7() {
    validateManifestFileName("a/ b/c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName8() {
    validateManifestFileName(" a/b/c");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidManifestFileName9() {
    validateManifestFileName("a /b/c");
  }

  private void validateManifestFileName(String fileName) {
    ManifestFile manifestFile = ManifestFile.builder().fileContent(FILE_CONTENT).fileName(fileName).build();
    manifestFile.setAppId(APP_ID);

    applicationManifestService.upsertApplicationManifestFile(manifestFile, applicationManifest, true);
  }

  @Test(expected = InvalidRequestException.class)
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
    applicationManifest = applicationManifestService.getByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
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
    applicationManifest = applicationManifestService.getByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
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
    applicationManifest = applicationManifestService.getByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNotNull();
  }

  @Test
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
    applicationManifest = applicationManifestService.getByServiceId(APP_ID, SERVICE_ID);
    assertThat(applicationManifest).isNull();
  }

  @Test
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
  public void testValidateManifestFileName() {
    upsertManifestFile("abc/def", "abc/pqr");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName1() {
    upsertManifestFile("abc/def", "abc");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName2() {
    upsertManifestFile("abc/def/ghi", "abc");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName3() {
    upsertManifestFile("abc/def/ghi", "abc/def");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName4() {
    upsertManifestFile("abc", "abc/def");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName5() {
    upsertManifestFile("abc/def", "abc/def/ghi");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName6() {
    upsertManifestFile("abc/def", "abc/def/ghi/klm");
  }

  @Test(expected = InvalidRequestException.class)
  public void testValidateManifestFileName7() {
    upsertManifestFile("abc/def", "abc/def");
  }

  @Test
  public void testValidateManifestFileName8() {
    upsertManifestFile("abc/def", "abc/ghi");
    upsertManifestFile("abc/jkl", "abc/mno");
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
}
