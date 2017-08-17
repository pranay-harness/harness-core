package software.wings.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static software.wings.beans.Application.Builder.anApplication;
import static software.wings.beans.Base.GLOBAL_ENV_ID;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ConfigFile.DEFAULT_TEMPLATE_ID;
import static software.wings.integration.IntegrationTestUtil.randomInt;
import static software.wings.utils.WingsTestConstants.FILE_NAME;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.Builder;
import software.wings.beans.EntityType;
import software.wings.beans.Service;
import software.wings.scheduler.JobScheduler;
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
import java.util.Arrays;
import javax.inject.Inject;

/**
 * Created by anubhaw on 6/19/17.
 */
public class ConfigFileIntegrationTest extends BaseIntegrationTest {
  public static final String INPUT_TEXT = "Input Text";
  @Mock private JobScheduler jobScheduler;
  @Inject private ConfigService configService;
  @Inject @InjectMocks private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private FileService fileService;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();
  private Application app;
  private Service service;

  private Builder configFileBuilder;

  @Before
  public void setUp() throws Exception {
    loginAdminUser();
    deleteAllDocuments(Arrays.asList(Application.class, ConfigFile.class, Service.class));

    app = appService.save(anApplication().withAccountId(accountId).withName("AppA").build());
    service =
        serviceResourceService.save(Service.Builder.aService().withAppId(app.getUuid()).withName("Catalog").build());
    configFileBuilder = aConfigFile()
                            .withAppId(service.getAppId())
                            .withAccountId(app.getAccountId())
                            .withName(FILE_NAME)
                            .withFileName(FILE_NAME)
                            .withEntityId(service.getUuid())
                            .withEntityType(EntityType.SERVICE)
                            .withTemplateId(DEFAULT_TEMPLATE_ID)
                            .withEnvId(GLOBAL_ENV_ID)
                            .withRelativeFilePath("tmp");
  }

  @Test
  public void shouldSaveServiceConfigFile() throws IOException {
    ConfigFile appConfigFile = configFileBuilder.but().build();

    FileInputStream fileInputStream = new FileInputStream(createRandomFile(FILE_NAME));
    String configId = configService.save(appConfigFile, new BoundedInputStream(fileInputStream));
    fileInputStream.close();
    ConfigFile configFile = configService.get(service.getAppId(), configId);
    assertThat(configFile).isNotNull().hasFieldOrPropertyWithValue("fileName", FILE_NAME);
  }

  @Test
  public void shouldUpdateServiceConfigFile() throws IOException {
    FileInputStream fileInputStream = new FileInputStream(createRandomFile(FILE_NAME));
    String configId = configService.save(configFileBuilder.but().build(), new BoundedInputStream(fileInputStream));
    fileInputStream.close();
    ConfigFile originalConfigFile = configService.get(service.getAppId(), configId);
    // update
    ConfigFile configFile = configFileBuilder.but().build();
    configFile.setUuid(configId);
    fileInputStream = new FileInputStream(createRandomFile(FILE_NAME + "_1"));
    configService.update(configFile, new BoundedInputStream(fileInputStream));
    ConfigFile updatedConfigFile = configService.get(service.getAppId(), configId);
    assertThat(updatedConfigFile).isNotNull().hasFieldOrPropertyWithValue("fileName", originalConfigFile.getFileName());
    assertThat(originalConfigFile.getFileUuid()).isNotEmpty().isNotEqualTo(updatedConfigFile.getFileUuid());
  }

  @Test
  public void shouldSaveEncryptedServiceConfigFile() throws IOException {
    ConfigFile appConfigFile = configFileBuilder.withEncrypted(true).but().build();
    InputStream inputStream = IOUtils.toInputStream(INPUT_TEXT, "ISO-8859-1");
    String configId = configService.save(appConfigFile, new BoundedInputStream(inputStream));
    inputStream.close();

    ConfigFile configFile = configService.get(service.getAppId(), configId);
    assertThat(configFile).isNotNull().hasFieldOrPropertyWithValue("fileName", FILE_NAME);

    File encryptedFile = testFolder.newFile();
    fileService.download(configFile.getFileUuid(), encryptedFile, FileBucket.CONFIGS);
    String encryptedText = String.join("", Files.readAllLines(encryptedFile.toPath(), Charset.forName("ISO-8859-1")));
    assertThat(encryptedText).isNotEmpty().isNotEqualTo(INPUT_TEXT);

    File decryptedFile = configService.download(configFile.getAppId(), configFile.getUuid());
    String decryptedText = String.join("", Files.readAllLines(decryptedFile.toPath(), Charset.forName("ISO-8859-1")));
    assertThat(decryptedText).isEqualTo(INPUT_TEXT);
  }

  private File createRandomFile(String fileName) throws IOException {
    File file = testFolder.newFile(fileName == null ? "randomfile " + randomInt() : fileName);
    BufferedWriter out = new BufferedWriter(new FileWriter(file));
    out.write("RandomText " + randomInt());
    out.close();
    return file;
  }
}
