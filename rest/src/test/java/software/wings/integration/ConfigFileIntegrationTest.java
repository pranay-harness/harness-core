package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.when;
import static org.mockito.internal.util.reflection.Whitebox.setInternalState;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.integration.IntegrationTestUtil.randomInt;

import com.google.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigFileBuilder;
import software.wings.beans.DelegateTask.SyncTaskContext;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.delegatetasks.DelegateProxyFactory;
import software.wings.scheduler.JobScheduler;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.impl.security.SecretManagementDelegateServiceImpl;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.utils.BoundedInputStream;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.UUID;

/**
 * Created by anubhaw on 6/19/17.
 */
public class ConfigFileIntegrationTest extends BaseIntegrationTest {
  public static final String INPUT_TEXT = "Input Text";
  @Mock private JobScheduler jobScheduler;
  @Inject private ConfigService configService;
  @Inject @InjectMocks private AppService appService;
  @Mock private DelegateProxyFactory delegateProxyFactory;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private FileService fileService;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private Application app;
  private Service service;
  private String fileName;

  private ConfigFileBuilder configFileBuilder;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    setInternalState(configService, "secretManager", secretManager);
    loginAdminUser();
    when(delegateProxyFactory.get(anyObject(), any(SyncTaskContext.class)))
        .thenReturn(new SecretManagementDelegateServiceImpl());

    app =
        appService.save(anApplication().withAccountId(accountId).withName("AppA" + System.currentTimeMillis()).build());
    service = serviceResourceService.save(
        Service.builder().appId(app.getUuid()).name("Catalog" + System.currentTimeMillis()).build());
    fileName = UUID.randomUUID().toString();
  }

  @Test
  public void shouldSaveServiceConfigFile() throws IOException {
    ConfigFile appConfigFile = getConfigFile();
    appConfigFile.setTemplateId(DEFAULT_TEMPLATE_ID);
    appConfigFile.setAppId(service.getAppId());
    appConfigFile.setName(fileName);
    appConfigFile.setFileName(fileName);

    FileInputStream fileInputStream = new FileInputStream(createRandomFile(fileName));
    String configId = configService.save(appConfigFile, new BoundedInputStream(fileInputStream));
    fileInputStream.close();
    ConfigFile configFile = configService.get(service.getAppId(), configId);
    assertThat(configFile).isNotNull().hasFieldOrPropertyWithValue("fileName", fileName);
  }

  @Test
  public void shouldUpdateServiceConfigFile() throws IOException {
    ConfigFile appConfigFile = getConfigFile();
    appConfigFile.setTemplateId(DEFAULT_TEMPLATE_ID);
    appConfigFile.setAppId(service.getAppId());
    appConfigFile.setName(fileName);
    appConfigFile.setFileName(fileName);

    FileInputStream fileInputStream = new FileInputStream(createRandomFile(fileName));
    String configId = configService.save(appConfigFile, new BoundedInputStream(fileInputStream));
    fileInputStream.close();
    ConfigFile originalConfigFile = configService.get(service.getAppId(), configId);

    // Update config
    ConfigFile configFile = getConfigFile();
    configFile.setTemplateId(DEFAULT_TEMPLATE_ID);
    configFile.setAppId(service.getAppId());
    configFile.setName(fileName);
    configFile.setFileName(fileName);
    configFile.setUuid(configId);

    fileInputStream = new FileInputStream(createRandomFile(fileName + "_1"));
    configService.update(configFile, new BoundedInputStream(fileInputStream));
    ConfigFile updatedConfigFile = configService.get(service.getAppId(), configId);
    assertThat(updatedConfigFile).isNotNull().hasFieldOrPropertyWithValue("fileName", originalConfigFile.getFileName());
    assertThat(originalConfigFile.getFileUuid()).isNotEmpty().isNotEqualTo(updatedConfigFile.getFileUuid());
  }

  @Test
  public void shouldOverrideSimpleConfigFile() throws IOException {
    ConfigFile configFile = getConfigFile();
    configFile.setAppId(service.getAppId());
    configFile.setName(fileName);
    configFile.setFileName(fileName);

    FileInputStream fileInputStream = new FileInputStream(createRandomFile(fileName));
    String configId = configService.save(configFile, new BoundedInputStream(fileInputStream));
    fileInputStream.close();
    ConfigFile originalConfigFile = configService.get(service.getAppId(), configId);

    // Update config file
    ConfigFile newConfigFile = getConfigFile();
    newConfigFile.setAppId(service.getAppId());
    newConfigFile.setName(fileName);
    newConfigFile.setFileName(fileName + "_1");
    newConfigFile.setUuid(configId);

    fileInputStream = new FileInputStream(createRandomFile(fileName + "_1"));
    configService.update(newConfigFile, new BoundedInputStream(fileInputStream));
    ConfigFile updatedConfigFile = configService.get(service.getAppId(), configId);

    // Original config checks
    assertThat(originalConfigFile.getDefaultVersion()).isEqualTo(1);
    assertThat(originalConfigFile.getFileUuid()).isNotEmpty().isNotEqualTo(updatedConfigFile.getFileUuid());
    assertThat(originalConfigFile.isEncrypted()).isFalse();

    // Updated config checks
    assertThat(updatedConfigFile.getDefaultVersion()).isEqualTo(2);
    assertThat(updatedConfigFile).isNotNull().hasFieldOrPropertyWithValue("fileName", updatedConfigFile.getFileName());
    assertThat(originalConfigFile.isEncrypted()).isFalse();
  }

  @Test
  public void shouldOverrideEncryptedConfigFile() throws IOException {
    ConfigFile configFile = getConfigFile();
    configFile.setEncrypted(true);

    String secretName = UUID.randomUUID().toString();
    InputStream inputStream = IOUtils.toInputStream(INPUT_TEXT, "ISO-8859-1");
    String secretFileId = secretManager.saveFile(
        accountId, secretName, null, new BoundedInputStream(new BoundedInputStream(inputStream)));

    configFile.setAppId(service.getAppId());
    configFile.setName(fileName);
    configFile.setFileName(fileName);
    configFile.setEncryptedFileId(secretFileId);
    String configId = configService.save(configFile, null);
    inputStream.close();

    ConfigFile originalConfigFile = configService.get(service.getAppId(), configId);
    assertThat(originalConfigFile).isNotNull().hasFieldOrPropertyWithValue("fileName", fileName);
    assertThat(originalConfigFile.getDefaultVersion()).isEqualTo(1);
    assertThat(originalConfigFile.isEncrypted()).isTrue();

    // Updated config file
    ConfigFile newConfigFile = getConfigFile();
    newConfigFile.setEncrypted(true);
    newConfigFile.setAppId(service.getAppId());
    newConfigFile.setName(fileName);
    newConfigFile.setFileName(fileName);
    newConfigFile.setUuid(configId);
    newConfigFile.setEncryptedFileId(secretFileId);

    configService.update(newConfigFile, new BoundedInputStream(null));
    ConfigFile updatedConfigFile = configService.get(service.getAppId(), configId);
    assertThat(updatedConfigFile).isNotNull().hasFieldOrPropertyWithValue("fileName", newConfigFile.getFileName());
    assertThat(updatedConfigFile.getDefaultVersion()).isEqualTo(2);
    assertThat(updatedConfigFile.isEncrypted()).isTrue();

    File decryptedFile = configService.download(configFile.getAppId(), configFile.getUuid());
    String decryptedText = String.join("", Files.readAllLines(decryptedFile.toPath(), Charset.forName("ISO-8859-1")));
    assertThat(decryptedText).isEqualTo(INPUT_TEXT);
  }

  @Test
  public void shouldSaveEncryptedServiceConfigFile() throws IOException {
    String secretName = UUID.randomUUID().toString();
    InputStream inputStream = IOUtils.toInputStream(INPUT_TEXT, "ISO-8859-1");
    String secretFileId = secretManager.saveFile(
        accountId, secretName, null, new BoundedInputStream(new BoundedInputStream(inputStream)));

    ConfigFile appConfigFile = getConfigFile();
    appConfigFile.setTemplateId(DEFAULT_TEMPLATE_ID);
    appConfigFile.setEncrypted(true);
    appConfigFile.setAppId(service.getAppId());
    appConfigFile.setName(fileName);
    appConfigFile.setFileName(fileName);
    appConfigFile.setEncryptedFileId(secretFileId);

    String configId = configService.save(appConfigFile, null);
    inputStream.close();

    ConfigFile configFile = configService.get(service.getAppId(), configId);
    assertThat(configFile).isNotNull().hasFieldOrPropertyWithValue("fileName", fileName);

    EncryptedData encryptedData = wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId());
    String savedFileId = String.valueOf(encryptedData.getEncryptedValue());

    File encryptedFile = testFolder.newFile();
    fileService.download(savedFileId, encryptedFile, FileBucket.CONFIGS);
    String encryptedText = String.join("", Files.readAllLines(encryptedFile.toPath(), Charset.forName("ISO-8859-1")));
    assertThat(encryptedText).isNotEmpty().isNotEqualTo(INPUT_TEXT);

    File decryptedFile = configService.download(configFile.getAppId(), configFile.getUuid());
    String decryptedText = String.join("", Files.readAllLines(decryptedFile.toPath(), Charset.forName("ISO-8859-1")));
    assertThat(decryptedText).isEqualTo(INPUT_TEXT);
  }

  private File createRandomFile(String fileName) throws IOException {
    File file = testFolder.newFile(fileName == null ? "randomfile " + randomInt() : fileName);
    try (BufferedWriter out = new BufferedWriter(new FileWriter(file))) {
      out.write("RandomText " + randomInt());
    }
    return file;
  }

  private ConfigFile getConfigFile() {
    return ConfigFile.builder()
        .accountId(app.getAccountId())
        .entityId(service.getUuid())
        .entityType(EntityType.SERVICE)
        .envId(GLOBAL_ENV_ID)
        .relativeFilePath("tmp")
        .build();
  }
}
