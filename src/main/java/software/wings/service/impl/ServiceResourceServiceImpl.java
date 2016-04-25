package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.Query;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;

import java.io.InputStream;
import java.util.List;
import javax.inject.Inject;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private FileService fileService;

  @Override
  public List<Service> list(String appId) {
    Application application = wingsPersistence.createQuery(Application.class)
                                  .field(ID_KEY)
                                  .equal(appId)
                                  .retrievedFields(true, "services")
                                  .get();
    return application.getServices();
  }

  public Service save(String appId, Service service) {
    Service savedService = wingsPersistence.saveAndGet(Service.class, service);
    wingsPersistence.addToList(Application.class, appId, "services", savedService);
    return savedService;
  }

  public Service findByUuid(String uuid) {
    return wingsPersistence.get(Service.class, uuid);
  }

  public Service update(Service service) {
    wingsPersistence.updateFields(Service.class, service.getUuid(),
        ImmutableMap.of("name", service.getName(), "description", service.getDescription(), "artifactType",
            service.getArtifactType()));
    return wingsPersistence.get(Service.class, service.getUuid());
  }

  @Override
  public List<ConfigFile> getConfigs(String serviceId) {
    Query<ConfigFile> query = wingsPersistence.createQuery(ConfigFile.class).field("serviceID").equal(serviceId);
    return query.asList();
  }

  @Override
  public String saveFile(ConfigFile configFile, InputStream uploadedInputStream, FileBucket configs) {
    fileService.saveFile(configFile, uploadedInputStream, configs);
    String configFileId = wingsPersistence.save(configFile);
    wingsPersistence.addToList(Service.class, configFile.getServiceId(), "configFiles", ImmutableMap.of());
    return configFileId;
  }

  @Override
  public ConfigFile getConfig(String configId) {
    return wingsPersistence.get(ConfigFile.class, configId);
  }

  @Override
  public void updateFile(ConfigFile configFile, InputStream uploadedInputStream, FileBucket configs) {
    fileService.saveFile(configFile, uploadedInputStream, configs);
    wingsPersistence.save(configFile);
  }
}
