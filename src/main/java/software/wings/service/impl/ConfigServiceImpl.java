package software.wings.service.impl;

import static software.wings.service.intfc.FileService.FileBucket.CONFIGS;

import org.mongodb.morphia.query.Query;
import software.wings.beans.ConfigFile;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.FileService;

import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 4/25/16.
 */
public class ConfigServiceImpl implements ConfigService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public List<ConfigFile> list(String entityId) {
    Query<ConfigFile> query = wingsPersistence.createQuery(ConfigFile.class).field("entityId").equal(entityId);
    return query.asList();
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
    fileService.saveFile(configFile, uploadedInputStream, CONFIGS);
    wingsPersistence.save(configFile); // FIXME: selective field update
  }

  @Override
  public void delete(String configId) {
    ConfigFile configFile = wingsPersistence.get(ConfigFile.class, configId);
    fileService.deleteFile(configFile.getFileUuid(), CONFIGS);
    wingsPersistence.delete(configFile);
  }

  @Override
  public List<ConfigFile> getConfigFilesByEntityId(String templateId, String entityId) {
    return wingsPersistence.createQuery(ConfigFile.class)
        .field("templateId")
        .equal(templateId)
        .field("entityId")
        .equal(entityId)
        .asList();
  }
}
