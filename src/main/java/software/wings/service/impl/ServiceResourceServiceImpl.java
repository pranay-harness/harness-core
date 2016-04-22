package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.inject.Inject;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Application;
import software.wings.beans.ConfigFile;
import software.wings.beans.Service;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.FileService;
import software.wings.service.intfc.FileService.FileBucket;
import software.wings.service.intfc.ServiceResourceService;

import java.io.InputStream;
import java.util.List;

/**
 * Created by anubhaw on 3/25/16.
 */
public class ServiceResourceServiceImpl implements ServiceResourceService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject FileService fileService;

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
    UpdateOperations<Application> updateOperations =
        wingsPersistence.createUpdateOperations(Application.class).add("services", savedService);
    Query<Application> updateQuery = wingsPersistence.createQuery(Application.class).field(ID_KEY).equal(appId);
    wingsPersistence.update(updateQuery, updateOperations);
    return savedService;
  }

  public Service findByUuid(String uuid) {
    return wingsPersistence.get(Service.class, uuid);
  }

  public Service update(Service service) {
    Query<Service> query = wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(service.getUuid());
    UpdateOperations<Service> operations = wingsPersistence.createUpdateOperations(Service.class)
                                               .set("name", service.getName())
                                               .set("description", service.getDescription())
                                               .set("artifactType", service.getArtifactType());
    wingsPersistence.update(query, operations);
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
    UpdateOperations<Service> updateOperations =
        wingsPersistence.createUpdateOperations(Service.class).add("configFiles", configFile);
    Query<Service> updateQuery =
        wingsPersistence.createQuery(Service.class).field(ID_KEY).equal(configFile.getServiceID());
    wingsPersistence.update(updateQuery, updateOperations);
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
