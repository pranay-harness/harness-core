package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import software.wings.beans.ConfigFile;
import software.wings.beans.PageRequest;
import software.wings.beans.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/25/16.
 */
public class ConfigServiceImpl implements ConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public PageResponse<ConfigFile> list(PageRequest<ConfigFile> request) {
    return wingsPersistence.query(ConfigFile.class, request);
  }

  @Override
  public String save(ConfigFile configFile, InputStream inputStream) {
    fileService.saveFile(configFile, inputStream, CONFIGS);
    return wingsPersistence.save(configFile);
  }

  @Override
  public ConfigFile get(String configId) {
    return wingsPersistence.get(ConfigFile.class, configId);
  }

  @Override
  public void update(ConfigFile configFile, InputStream uploadedInputStream) {
    if (uploadedInputStream != null) {
      fileService.saveFile(configFile, uploadedInputStream, CONFIGS);
    }

    Map<String, Object> updateMap = new HashMap<>();
    updateMap.put("name", configFile.getName());
    updateMap.put("relativePath", configFile.getRelativePath());
    if (configFile.getChecksum() != null) {
      updateMap.put("checksum", configFile.getChecksum());
    }
    wingsPersistence.updateFields(ConfigFile.class, configFile.getUuid(), updateMap);
  }

  @Override
  public void delete(String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, configId);
    fileService.deleteFile(configFile.getFileUuid(), CONFIGS);
    wingsPersistence.delete(configFile);
  }

  @Override
  public List<ConfigFile> getConfigFilesForEntity(String templateId, String entityId) {
    List<ConfigFile> configFiles = wingsPersistence.createQuery(ConfigFile.class)
                                       .field("templateId")
                                       .equal(templateId)
                                       .field("entityId")
                                       .equal(entityId)
                                       .asList();
    return configFiles != null ? configFiles : new ArrayList<>();
  }
}
