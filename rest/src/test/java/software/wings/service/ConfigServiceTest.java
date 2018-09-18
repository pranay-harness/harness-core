package software.wings.service;

import static io.harness.beans.PageRequest.PageRequestBuilder.aPageRequest;
import static io.harness.beans.SearchFilter.Operator.EQ;
import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.utils.WingsTestConstants.ACCOUNT_ID;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

import com.google.inject.Inject;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.beans.SearchFilter.Operator;
import io.harness.exception.WingsException;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mongodb.morphia.query.FieldEnd;
import org.mongodb.morphia.query.Query;
import software.wings.WingsBaseTest;
import software.wings.beans.ConfigFile;
import software.wings.beans.EntityType;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.WingsPersistence;
import software.wings.security.encryption.EncryptedData;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;
import software.wings.utils.BoundedInputStream;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Created by anubhaw on 8/9/16.
 */
public class ConfigServiceTest extends WingsBaseTest {
  /**
   * The Query.
   */
  @Mock Query<ConfigFile> query;
  /**
   * The End.
   */
  @Mock FieldEnd end;
  @Mock private WingsPersistence wingsPersistence;
  @Mock private FileService fileService;
  @Mock private HostService hostService;
  @Mock private ServiceResourceService serviceResourceService;
  @Mock private ServiceTemplateService serviceTemplateService;

  @Inject @InjectMocks private ConfigService configService;
  private InputStream inputStream;

  /**
   * Sets up.
   *
   * @throws IOException the io exception
   */
  @Before
  public void setUp() throws IOException {
    inputStream = IOUtils.toInputStream("Some content", "UTF-8");
    when(wingsPersistence.createQuery(ConfigFile.class)).thenReturn(query);
    when(query.filter(any(), any())).thenReturn(query);
    when(query.get()).thenReturn(ConfigFile.builder().build());
  }

  /**
   * Should list.
   */
  @Test
  public void shouldList() {
    ConfigFile configFile = ConfigFile.builder()
                                .entityType(EntityType.SERVICE)
                                .entityId(SERVICE_ID)
                                .templateId(ConfigFile.DEFAULT_TEMPLATE_ID)
                                .build();

    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);
    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1l);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter("appId", EQ, APP_ID)
                                  .addFilter("envId", EQ, ENV_ID)
                                  .addFilter("templateId", EQ, TEMPLATE_ID)
                                  .addFilter("entityId", EQ, "ENTITY_ID")
                                  .build();

    when(wingsPersistence.query(ConfigFile.class, pageRequest)).thenReturn(pageResponse);
    PageResponse<ConfigFile> configFiles = configService.list(pageRequest);
    assertThat(configFiles).isNotNull();
    assertThat(configFiles.getResponse().get(0)).isInstanceOf(ConfigFile.class);
  }

  /**
   * Should save.
   */
  @Test
  public void shouldSave() {
    when(serviceTemplateService.get(APP_ID, TEMPLATE_ID))
        .thenReturn(aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build());
    when(serviceTemplateService.exist(APP_ID, TEMPLATE_ID)).thenReturn(true);
    ConfigFile configFile = ConfigFile.builder()
                                .accountId(ACCOUNT_ID)
                                .envId(ENV_ID)
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .relativeFilePath("PATH/" + FILE_NAME)
                                .build();
    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);
    configFile.setUuid(FILE_ID);
    BoundedInputStream inputStream = new BoundedInputStream(this.inputStream);
    when(wingsPersistence.save(configFile)).thenReturn(FILE_ID);
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    configService.save(configFile, inputStream);
    verify(fileService).saveFile(configFile, inputStream, FileBucket.CONFIGS);
    assertThat(configFile.getRelativeFilePath()).isEqualTo("PATH/" + FILE_NAME);
    verify(wingsPersistence).save(configFile);
  }

  /**
   * Should throw exception for unsupported entity types.
   */
  @Test
  public void shouldThrowExceptionForUnsupportedEntityTypes() {
    ConfigFile configFile = ConfigFile.builder()
                                .accountId(ACCOUNT_ID)
                                .entityType(EntityType.ENVIRONMENT)
                                .entityId(ENV_ID)
                                .templateId(TEMPLATE_ID)
                                .build();
    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);
    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> configService.save(configFile, new BoundedInputStream(inputStream)));
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    ConfigFile configFile = ConfigFile.builder().build();
    configFile.setAppId(APP_ID);
    configFile.setUuid(FILE_ID);
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);

    configFile = configService.get(APP_ID, FILE_ID);
    verify(wingsPersistence).get(ConfigFile.class, APP_ID, FILE_ID);
    assertThat(configFile.getUuid()).isEqualTo(FILE_ID);
  }

  /**
   * Should get config file by template.
   */
  @Test
  public void shouldGetConfigFileByTemplate() {
    ServiceTemplate serviceTemplate =
        aServiceTemplate().withAppId(APP_ID).withEnvId(ENV_ID).withUuid(TEMPLATE_ID).build();
    ConfigFile configFile = ConfigFile.builder()
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .build();
    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);

    when(query.asList()).thenReturn(asList(configFile));
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    List<ConfigFile> configFiles = configService.getConfigFileByTemplate(APP_ID, ENV_ID, serviceTemplate.getUuid());

    verify(query).filter("appId", APP_ID);
    verify(query).filter("envId", ENV_ID);
    verify(query).filter("templateId", TEMPLATE_ID);
    assertThat(configFiles.get(0)).isEqualTo(configFile);
  }

  /**
   * Should download.
   */
  @Test
  public void shouldDownload() {
    ConfigFile configFile = ConfigFile.builder()
                                .envId(ENV_ID)
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .relativeFilePath("PATH")
                                .build();
    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);
    configFile.setUuid(FILE_ID);
    configFile.setFileUuid("GFS_FILE_ID");
    configFile.setChecksum("CHECKSUM");
    configFile.setSize(12);

    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    File file = configService.download(APP_ID, FILE_ID);
    verify(wingsPersistence).get(ConfigFile.class, APP_ID, FILE_ID);
    verify(fileService).download(eq("GFS_FILE_ID"), any(File.class), eq(FileBucket.CONFIGS));
    assertThat(file.getName()).isEqualTo("PATH");
  }

  /**
   * Should update.
   */
  @Test
  public void shouldUpdate() {
    ConfigFile configFile = ConfigFile.builder()
                                .accountId(ACCOUNT_ID)
                                .envId(ENV_ID)
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .relativeFilePath("PATH")
                                .encrypted(true)
                                .build();
    configFile.setAppId(APP_ID);
    configFile.setUuid(FILE_ID);
    configFile.setFileUuid("GFS_FILE_ID");
    configFile.setFileName(FILE_NAME);
    configFile.setChecksum("CHECKSUM");
    configFile.setSize(12);
    configFile.setEncryptedFileId("ENC_ID");
    configFile.setName("Name00");

    when(wingsPersistence.get(EncryptedData.class, configFile.getEncryptedFileId()))
        .thenReturn(EncryptedData.builder().encryptedValue("csd".toCharArray()).build());
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    BoundedInputStream boundedInputStream = new BoundedInputStream(this.inputStream);
    configService.update(configFile, boundedInputStream);
    ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(wingsPersistence).updateFields(eq(ConfigFile.class), eq(FILE_ID), argumentCaptor.capture());

    Map<String, Object> updateMap = argumentCaptor.getValue();
    assertThat(updateMap.size()).isEqualTo(6);
    assertThat(updateMap.get("encrypted")).isEqualTo(true);
    assertThat(updateMap.get("encryptedFileId")).isNotNull();
    assertThat(updateMap.get("size")).isNotNull();
  }

  /**
   * Should delete.
   */
  @Test
  public void shouldDelete() {
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);
    when(wingsPersistence.createQuery(ConfigFile.class)).thenReturn(query);
    configService.delete(APP_ID, FILE_ID);
    verify(wingsPersistence).delete(query);
    verify(fileService).deleteAllFilesForEntity(FILE_ID, FileBucket.CONFIGS);
  }

  /**
   * Should get config files for entity.
   */
  @Test
  public void shouldGetConfigFilesForEntity() {
    ConfigFile configFile = ConfigFile.builder()
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .build();

    configFile.setAppId(APP_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileName(FILE_NAME);
    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1l);

    PageRequest<ConfigFile> pageRequest = aPageRequest()
                                              .addFilter("appId", Operator.EQ, APP_ID)
                                              .addFilter("templateId", Operator.EQ, TEMPLATE_ID)
                                              .addFilter("entityId", Operator.EQ, "ENTITY_ID")
                                              .build();

    when(wingsPersistence.query(ConfigFile.class, pageRequest)).thenReturn(pageResponse);

    configService.getConfigFilesForEntity(APP_ID, TEMPLATE_ID, "ENTITY_ID");
    verify(wingsPersistence).query(ConfigFile.class, pageRequest);
  }

  /**
   * Should delete by entity id.
   */
  @Test
  public void shouldDeleteByEntityId() {
    ConfigFile configFile = ConfigFile.builder()
                                .envId(ENV_ID)
                                .entityType(EntityType.SERVICE_TEMPLATE)
                                .entityId(TEMPLATE_ID)
                                .templateId(TEMPLATE_ID)
                                .relativeFilePath("PATH")
                                .build();

    configFile.setAppId(APP_ID);
    configFile.setUuid(FILE_ID);
    configFile.setName(FILE_NAME);
    configFile.setFileUuid("GFS_FILE_ID");
    configFile.setFileName(FILE_NAME);
    configFile.setChecksum("CHECKSUM");
    configFile.setSize(12);

    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1l);

    PageRequest<ConfigFile> pageRequest = aPageRequest()
                                              .addFilter("appId", Operator.EQ, APP_ID)
                                              .addFilter("templateId", Operator.EQ, TEMPLATE_ID)
                                              .addFilter("entityId", Operator.EQ, "ENTITY_ID")
                                              .build();

    when(wingsPersistence.createQuery(ConfigFile.class)).thenReturn(query);
    when(wingsPersistence.query(ConfigFile.class, pageRequest)).thenReturn(pageResponse);
    when(wingsPersistence.delete(any(Query.class))).thenReturn(true);

    configService.deleteByEntityId(APP_ID, TEMPLATE_ID, "ENTITY_ID");
    verify(wingsPersistence).delete(query);
    verify(fileService).deleteAllFilesForEntity(FILE_ID, FileBucket.CONFIGS);
  }

  /**
   * Should validate and resolve file path.
   *
   * @throws Exception the exception
   */
  @Test
  public void shouldValidateAndResolveFilePath() throws Exception {
    assertThat(configService.validateAndResolveFilePath("config/abc.txt")).isEqualTo("config/abc.txt");
    assertThat(configService.validateAndResolveFilePath("./config/abc.txt")).isEqualTo("config/abc.txt");
    assertThat(configService.validateAndResolveFilePath("./config/./abc.txt")).isEqualTo("config/abc.txt");
    assertThat(configService.validateAndResolveFilePath("./config/./abc.txt")).isEqualTo("config/abc.txt");
    assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> configService.validateAndResolveFilePath("/config"))
        .withMessage(INVALID_ARGUMENT.name());
  }
}
