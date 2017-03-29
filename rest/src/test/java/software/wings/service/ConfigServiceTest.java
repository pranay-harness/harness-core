package software.wings.service;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static software.wings.beans.ConfigFile.Builder.aConfigFile;
import static software.wings.beans.ErrorCode.INVALID_ARGUMENT;
import static software.wings.beans.SearchFilter.Builder.aSearchFilter;
import static software.wings.beans.SearchFilter.Operator.EQ;
import static software.wings.beans.ServiceTemplate.Builder.aServiceTemplate;
import static software.wings.dl.PageRequest.Builder.aPageRequest;
import static software.wings.utils.WingsTestConstants.APP_ID;
import static software.wings.utils.WingsTestConstants.ENV_ID;
import static software.wings.utils.WingsTestConstants.FILE_ID;
import static software.wings.utils.WingsTestConstants.FILE_NAME;
import static software.wings.utils.WingsTestConstants.SERVICE_ID;
import static software.wings.utils.WingsTestConstants.TEMPLATE_ID;

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
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.ServiceTemplate;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ServiceTemplateService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

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
    when(query.field(any())).thenReturn(end);
    when(end.equal(any())).thenReturn(query);
  }

  /**
   * Should listStateMachines.
   */
  @Test
  public void shouldList() {
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEntityType(EntityType.SERVICE)
                                .withEntityId(SERVICE_ID)
                                .withTemplateId(ConfigFile.DEFAULT_TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withFileName(FILE_NAME)
                                .build();

    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1);

    PageRequest pageRequest = aPageRequest()
                                  .withLimit("50")
                                  .withOffset("0")
                                  .addFilter(aSearchFilter()
                                                 .withField("appId", EQ, APP_ID)
                                                 .withField("envId", EQ, ENV_ID)
                                                 .withField("templateId", EQ, TEMPLATE_ID)
                                                 .withField("entityId", EQ, "ENTITY_ID")
                                                 .build())
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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEnvId(ENV_ID)
                                .withUuid(FILE_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName("NAME")
                                .withRelativeFilePath("PATH/" + FILE_NAME)
                                .withFileName(FILE_NAME)
                                .build();
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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEntityType(EntityType.ENVIRONMENT)
                                .withEntityId(ENV_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withFileName(FILE_NAME)
                                .build();
    Assertions.assertThatExceptionOfType(WingsException.class)
        .isThrownBy(() -> configService.save(configFile, inputStream));
  }

  /**
   * Should get.
   */
  @Test
  public void shouldGet() {
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID))
        .thenReturn(aConfigFile().withAppId(APP_ID).withUuid(FILE_ID).build());
    ConfigFile configFile = configService.get(APP_ID, FILE_ID);
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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withFileName(FILE_NAME)
                                .build();

    when(query.asList()).thenReturn(Arrays.asList(configFile));
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    List<ConfigFile> configFiles = configService.getConfigFileByTemplate(APP_ID, ENV_ID, serviceTemplate);

    verify(query).field("appId");
    verify(end).equal(APP_ID);
    verify(query).field("envId");
    verify(end).equal(ENV_ID);
    verify(query).field("templateId");
    verify(end).equal(TEMPLATE_ID);
    assertThat(configFiles.get(0)).isEqualTo(configFile);
  }

  /**
   * Should download.
   */
  @Test
  public void shouldDownload() {
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEnvId(ENV_ID)
                                .withUuid(FILE_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withRelativeFilePath("PATH")
                                .withFileUuid("GFS_FILE_ID")
                                .withFileName(FILE_NAME)
                                .withChecksum("CHECKSUM")
                                .withSize(100)
                                .build();

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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEnvId(ENV_ID)
                                .withUuid(FILE_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withRelativeFilePath("PATH")
                                .withFileUuid("GFS_FILE_ID")
                                .withFileName(FILE_NAME)
                                .withChecksum("CHECKSUM")
                                .withSize(100)
                                .build();
    when(wingsPersistence.get(ConfigFile.class, APP_ID, FILE_ID)).thenReturn(configFile);
    configService.update(configFile, inputStream);
    verify(fileService).saveFile(configFile, inputStream, FileBucket.CONFIGS);
    ArgumentCaptor<Map> argumentCaptor = ArgumentCaptor.forClass(Map.class);
    verify(wingsPersistence).updateFields(eq(ConfigFile.class), eq(FILE_ID), argumentCaptor.capture());

    Map<String, Object> updateMap = argumentCaptor.getValue();
    assertThat(updateMap.size()).isEqualTo(7);
    assertThat(updateMap.get("fileUuid")).isEqualTo("GFS_FILE_ID");
    assertThat(updateMap.get("checksum")).isEqualTo("CHECKSUM");
    assertThat(updateMap.get("size")).isEqualTo(100L);
    assertThat(updateMap.get("fileName")).isEqualTo(FILE_NAME);
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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withFileName(FILE_NAME)
                                .build();

    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1);

    PageRequest<ConfigFile> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("templateId", Operator.EQ, TEMPLATE_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
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
    ConfigFile configFile = aConfigFile()
                                .withAppId(APP_ID)
                                .withEnvId(ENV_ID)
                                .withUuid(FILE_ID)
                                .withEntityType(EntityType.SERVICE_TEMPLATE)
                                .withEntityId(TEMPLATE_ID)
                                .withTemplateId(TEMPLATE_ID)
                                .withName(FILE_NAME)
                                .withRelativeFilePath("PATH")
                                .withFileUuid("GFS_FILE_ID")
                                .withFileName(FILE_NAME)
                                .withChecksum("CHECKSUM")
                                .withSize(100)
                                .build();

    PageResponse<ConfigFile> pageResponse = new PageResponse<>();
    pageResponse.setResponse(asList(configFile));
    pageResponse.setTotal(1);

    PageRequest<ConfigFile> pageRequest =
        aPageRequest()
            .addFilter(aSearchFilter().withField("appId", Operator.EQ, APP_ID).build())
            .addFilter(aSearchFilter().withField("templateId", Operator.EQ, TEMPLATE_ID).build())
            .addFilter(aSearchFilter().withField("entityId", Operator.EQ, "ENTITY_ID").build())
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
