package software.wings.integration;

import static javax.ws.rs.client.Entity.entity;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import io.harness.data.structure.EmptyPredicate;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.glassfish.jersey.media.multipart.file.FileDataBodyPart;
import org.junit.Before;
import org.junit.Test;
import software.wings.beans.ConfigFile;
import software.wings.beans.ConfigFile.ConfigOverrideType;
import software.wings.beans.EntityType;
import software.wings.beans.RestResponse;

import java.io.File;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

/**
 * @author marklu on 2019-01-23
 */
public class ConfigResourceIntegrationTest extends BaseIntegrationTest {
  private static final String TEST_APPLICATION = "Test Application";
  private static final String TEST_SERVICE = "Test Service";
  private static final String TEST_ENVIRONMENT = "Test Environment";

  private static final String CONFIG_FILES_PATH = "configfiles/";
  private static final String CONFIG_FILE1 = "config1.txt";
  private static final String CONFIG_FILE2 = "config2.txt";
  private static final String CONFIG_FILE_OVERRIDE1 = "config_override1.txt";
  private static final String CONFIG_FILE_OVERRIDE2 = "config_override2.txt";

  private static final String CONFIG_FILE1_CONTENT = "Config file 1";
  private static final String CONFIG_FILE2_CONTENT = "Config file 2";
  private static final String CONFIG_FILE_OVERRIDE1_CONTENT = "Config file override 1";
  private static final String CONFIG_FILE_OVERRIDE2_CONTENT = "Config file override 2";

  private String appId;
  private String serviceId;
  private String envId;
  private String templateId;

  @Before
  public void setUp() throws Exception {
    super.setUp();
    loginAdminUser();

    // "Test Application" is the application created by DataGen utility
    appId = appService.getAppByName(accountId, TEST_APPLICATION).getUuid();
    serviceId = serviceResourceService.getServiceByName(appId, TEST_SERVICE).getUuid();
    envId = environmentService.getEnvironmentByName(appId, TEST_ENVIRONMENT).getUuid();
    templateId = serviceTemplateService.get(appId, serviceId, envId).getUuid();
  }

  @Test
  public void testServiceLevelConfigFileCrud() {
    // 1. Create a new service level config file
    String relativePath = "configFile1";
    ConfigFile configFile = createConfigFile(relativePath, relativePath, CONFIG_FILE1, false);
    String configId = createConfig(appId, serviceId, EntityType.SERVICE, CONFIG_FILE1, configFile);

    // 2. Retrieved to make sure the config file is saved properly
    ConfigFile savedConfigFile = getConfig(appId, configId);
    int oldConfigFileVersion = savedConfigFile.getDefaultVersion();

    // 3. Download the config file to verify content
    String downloadedFileContent = downloadConfig(configId, appId, oldConfigFileVersion);
    assertEquals(CONFIG_FILE1_CONTENT, downloadedFileContent);

    // 4. Update the same config file with different content
    savedConfigFile.setFileName(CONFIG_FILE2);
    updateConfig(configId, appId, serviceId, EntityType.SERVICE, CONFIG_FILE2, savedConfigFile);

    // 5. Download the config file again to verify content changed
    savedConfigFile = getConfig(appId, configId);
    int newConfigFileVersion = savedConfigFile.getDefaultVersion();
    assertNotEquals(oldConfigFileVersion, newConfigFileVersion);
    downloadedFileContent = downloadConfig(configId, appId, newConfigFileVersion);
    assertEquals(CONFIG_FILE2_CONTENT, downloadedFileContent);

    // 6. Delete the config file
    deleteConfig(configId, appId);

    // 7. Verify the config file is deleted
    savedConfigFile = wingsPersistence.getWithAppId(ConfigFile.class, appId, configId);
    assertNull(savedConfigFile);
  }

  @Test
  public void testMultipleConfigFileUpdate_shouldNot_OverwriteEachOther() {
    // 1. Create 2 new service level config files
    String relativePath1 = "configFile1";
    String relativePath2 = "configFile2";
    ConfigFile configFile1 = createConfigFile(relativePath1, relativePath1, CONFIG_FILE1, false);
    ConfigFile configFile2 = createConfigFile(relativePath2, relativePath2, CONFIG_FILE2, false);
    String configId1 = createConfig(appId, serviceId, EntityType.SERVICE, CONFIG_FILE1, configFile1);
    String configId2 = createConfig(appId, serviceId, EntityType.SERVICE, CONFIG_FILE2, configFile2);

    // 2. Update the 2nd config file multiple times.
    ConfigFile savedConfigFile2 = getConfig(appId, configId2);
    savedConfigFile2.setFileName(CONFIG_FILE_OVERRIDE1);
    updateConfig(configId2, appId, serviceId, EntityType.SERVICE, CONFIG_FILE_OVERRIDE1, savedConfigFile2);
    savedConfigFile2.setFileName(CONFIG_FILE_OVERRIDE2);
    updateConfig(configId2, appId, serviceId, EntityType.SERVICE, CONFIG_FILE_OVERRIDE2, savedConfigFile2);

    // 3. Verify 2nd config file has the latest content
    savedConfigFile2 = getConfig(appId, configId2);
    String downloadedFileContent = downloadConfig(configId2, appId, savedConfigFile2.getDefaultVersion());
    assertEquals(CONFIG_FILE_OVERRIDE2_CONTENT, downloadedFileContent);
    // verify again using another file content download API from ConfigService
    downloadedFileContent = new String(configService.getFileContent(appId, savedConfigFile2));
    assertEquals(CONFIG_FILE_OVERRIDE2_CONTENT, downloadedFileContent);

    // 4. Verify 1st config file content has not changed
    ConfigFile savedConfigFile1 = getConfig(appId, configId1);
    downloadedFileContent = downloadConfig(configId1, appId, savedConfigFile1.getDefaultVersion());
    assertEquals(CONFIG_FILE1_CONTENT, downloadedFileContent);

    // 5. Delete all config files
    deleteConfig(configId1, appId);
    deleteConfig(configId2, appId);

    // 6. Verified all config files deleted
    savedConfigFile1 = wingsPersistence.getWithAppId(ConfigFile.class, appId, configId1);
    assertNull(savedConfigFile1);
    savedConfigFile2 = wingsPersistence.getWithAppId(ConfigFile.class, appId, configId1);
    assertNull(savedConfigFile2);
  }

  @Test
  public void testEnvironmentLevelConfigFileOverrideCrud() {
    // 1. Create a new service level config file
    String relativePath = "configFile1";
    ConfigFile configFile1 = createConfigFile(relativePath, relativePath, CONFIG_FILE1, false);
    String configId = createConfig(appId, serviceId, EntityType.SERVICE, CONFIG_FILE1, configFile1);

    // 2. Create a config override.
    String overrideRelativePath = "configOverride1";
    ConfigFile configOverrideFile = createConfigFile(overrideRelativePath, overrideRelativePath, CONFIG_FILE_OVERRIDE1,
        false, configId, templateId, ConfigOverrideType.ALL, true, true);
    String configOverrideId =
        createConfig(appId, templateId, EntityType.SERVICE_TEMPLATE, CONFIG_FILE_OVERRIDE1, configOverrideFile);

    // 3. Update override file content
    configOverrideFile.setFileName(CONFIG_FILE_OVERRIDE2);
    updateConfig(
        configOverrideId, appId, templateId, EntityType.SERVICE_TEMPLATE, CONFIG_FILE_OVERRIDE2, configOverrideFile);

    // 4. Verified the original config file content is expected
    ConfigFile savedConfigFile = getConfig(appId, configId);
    String downloadedFileContent = downloadConfig(configId, appId, savedConfigFile.getDefaultVersion());
    assertEquals(CONFIG_FILE1_CONTENT, downloadedFileContent);

    // 5. Verify the config override file content is expected
    ConfigFile savedOverrideConfigFile = getConfig(appId, configOverrideId);
    String downloadedOverrideFileContent =
        downloadConfig(configOverrideId, appId, savedOverrideConfigFile.getDefaultVersion());
    assertEquals(CONFIG_FILE_OVERRIDE2_CONTENT, downloadedOverrideFileContent);

    // 6. Delete the config file
    deleteConfig(configId, appId);
    deleteConfig(configOverrideId, appId);

    // 7. Verify the config file is deleted
    savedConfigFile = wingsPersistence.getWithAppId(ConfigFile.class, appId, configId);
    assertNull(savedConfigFile);
    savedOverrideConfigFile = wingsPersistence.getWithAppId(ConfigFile.class, appId, configOverrideId);
    assertNull(savedOverrideConfigFile);
  }

  private String createConfig(
      String appId, String entityId, EntityType entityType, String fileName, ConfigFile configFile) {
    String url = API_BASE + "/configs?appId=" + appId + "&entityId=" + entityId + "&entityType=" + entityType;
    return (String) uploadConfig(url, true, fileName, configFile);
  }

  private String updateConfig(
      String configId, String appId, String entityId, EntityType entityType, String fileName, ConfigFile configFile) {
    String url =
        API_BASE + "/configs/" + configId + "?appId=" + appId + "&entityId=" + entityId + "&entityType=" + entityType;
    return (String) uploadConfig(url, false, fileName, configFile);
  }

  private Object uploadConfig(String url, boolean create, String fileName, ConfigFile configFile) {
    File fileToImport = new File(getClass().getClassLoader().getResource(CONFIG_FILES_PATH + fileName).getFile());

    MultiPart multiPart = new MultiPart();
    multiPart.bodyPart(new FileDataBodyPart("file", fileToImport, MediaType.MULTIPART_FORM_DATA_TYPE));
    multiPart.bodyPart(new FormDataBodyPart("fileName", fileName, MediaType.MULTIPART_FORM_DATA_TYPE));

    multiPart.bodyPart(new FormDataBodyPart(
        "encrypted", String.valueOf(configFile.isEncrypted()), MediaType.MULTIPART_FORM_DATA_TYPE));
    multiPart.bodyPart(
        new FormDataBodyPart("description", configFile.getDescription(), MediaType.MULTIPART_FORM_DATA_TYPE));
    multiPart.bodyPart(new FormDataBodyPart(
        "targetToAllEnv", String.valueOf(configFile.isTargetToAllEnv()), MediaType.MULTIPART_FORM_DATA_TYPE));
    multiPart.bodyPart(
        new FormDataBodyPart("relativeFilePath", configFile.getRelativeFilePath(), MediaType.MULTIPART_FORM_DATA_TYPE));
    multiPart.bodyPart(new FormDataBodyPart(
        "setAsDefault", String.valueOf(configFile.isSetAsDefault()), MediaType.MULTIPART_FORM_DATA_TYPE));
    if (EmptyPredicate.isNotEmpty(configFile.getNotes())) {
      multiPart.bodyPart(new FormDataBodyPart("notes", configFile.getNotes(), MediaType.MULTIPART_FORM_DATA_TYPE));
    }
    if (EmptyPredicate.isNotEmpty(configFile.getTemplateId())) {
      multiPart.bodyPart(
          new FormDataBodyPart("templateId", configFile.getTemplateId(), MediaType.MULTIPART_FORM_DATA_TYPE));
    }
    if (EmptyPredicate.isNotEmpty(configFile.getParentConfigFileId())) {
      multiPart.bodyPart(new FormDataBodyPart(
          "parentConfigFileId", configFile.getParentConfigFileId(), MediaType.MULTIPART_FORM_DATA_TYPE));
    }
    if (configFile.getConfigOverrideType() != null) {
      multiPart.bodyPart(new FormDataBodyPart(
          "configOverrideType", configFile.getConfigOverrideType().name(), MediaType.MULTIPART_FORM_DATA_TYPE));
    }

    WebTarget target = client.target(url);
    final RestResponse<Object> restResponse;
    if (create) {
      restResponse = getRequestBuilderWithAuthHeader(target).post(
          entity(multiPart, MediaType.MULTIPART_FORM_DATA), new GenericType<RestResponse<Object>>() {});
      assertEquals(0, restResponse.getResponseMessages().size());
      assertNotNull(restResponse.getResource());
    } else {
      restResponse = getRequestBuilderWithAuthHeader(target).put(
          entity(multiPart, MediaType.MULTIPART_FORM_DATA), new GenericType<RestResponse<Object>>() {});
      assertEquals(0, restResponse.getResponseMessages().size());
    }

    return restResponse.getResource();
  }

  private void deleteConfig(String configId, String appId) {
    WebTarget target = client.target(API_BASE + "/configs/" + configId + "?appId=" + appId);
    RestResponse<Void> restResponse =
        getRequestBuilderWithAuthHeader(target).delete(new GenericType<RestResponse<Void>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
  }

  private ConfigFile getConfig(String appId, String configId) {
    WebTarget target = client.target(API_BASE + "/configs/" + configId + "?appId=" + appId);
    RestResponse<ConfigFile> restResponse =
        getRequestBuilderWithAuthHeader(target).get(new GenericType<RestResponse<ConfigFile>>() {});
    assertEquals(0, restResponse.getResponseMessages().size());
    assertNotNull(restResponse.getResource());

    return restResponse.getResource();
  }

  private String downloadConfig(String configId, String appId, int version) {
    WebTarget target =
        client.target(API_BASE + "/configs/" + configId + "/download?appId=" + appId + "&version=" + version);
    String restResponse = getRequestBuilderWithAuthHeader(target).get(new GenericType<String>() {});
    assertNotNull(restResponse);

    return restResponse;
  }

  private ConfigFile createConfigFile(String relativePath, String description, String fileName, boolean encrypted) {
    return createConfigFile(relativePath, description, fileName, encrypted, null, null, null, true, true);
  }

  private ConfigFile createConfigFile(String relativePath, String description, String fileName, boolean encrypted,
      String parentConfigFileId, String templateId, ConfigOverrideType configOverrideType, boolean targetToAllEnv,
      boolean setAsDefault) {
    ConfigFile configFile = ConfigFile.builder()
                                .relativeFilePath(relativePath)
                                .description(description)
                                .encrypted(encrypted)
                                .targetToAllEnv(targetToAllEnv)
                                .setAsDefault(setAsDefault)
                                .build();
    configFile.setFileName(fileName);
    if (parentConfigFileId != null) {
      configFile.setParentConfigFileId(parentConfigFileId);
    }
    if (templateId != null) {
      configFile.setTemplateId(templateId);
    }
    if (configOverrideType != null) {
      configFile.setConfigOverrideType(configOverrideType);
    }

    return configFile;
  }
}
