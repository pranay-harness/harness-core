package software.wings.service.impl;

import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;

import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Environment;
import software.wings.beans.ErrorCode;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/9/16.
 */
@ValidateOnExecution
@Singleton
public class HostServiceImpl implements HostService {
  private final Logger logger = LoggerFactory.getLogger(getClass());
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;
  @Inject private NotificationService notificationService;
  @Inject private EnvironmentService environmentService;
  @Inject private ConfigService configService;
  @Inject private ExecutorService executorService;

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#list(software.wings.dl.PageRequest)
   */
  @Override
  public PageResponse<Host> list(PageRequest<Host> req) {
    return wingsPersistence.query(Host.class, req);
  }

  @Override
  public Host get(String appId, String envId, String hostId) {
    Host host = wingsPersistence.createQuery(Host.class)
                    .field(ID_KEY)
                    .equal(hostId)
                    .field("envId")
                    .equal(envId)
                    .field("appId")
                    .equal(appId)
                    .get();
    notNullCheck("Host", host);
    return host;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#update(software.wings.beans.infrastructure.Host)
   */
  @Override
  public Host update(String envId, Host host) {
    Host savedHost = get(host.getAppId(), envId, host.getUuid());

    if (savedHost == null) {
      throw new WingsException(ErrorCode.INVALID_REQUEST, "message", "Host doesn't exist");
    }

    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder().put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    wingsPersistence.updateFields(Host.class, savedHost.getUuid(), builder.build());

    Host appHost = get(savedHost.getAppId(), savedHost.getEnvId(), host.getUuid());
    return appHost;
  }

  @Override
  public Host saveHost(Host appHost) {
    Host applicationHost = wingsPersistence.createQuery(Host.class)
                               .field("hostName")
                               .equal(appHost.getHostName())
                               .field("appId")
                               .equal(appHost.getAppId())
                               .field("envId")
                               .equal(appHost.getEnvId())
                               .field("serviceTemplateId")
                               .equal(appHost.getServiceTemplateId())
                               .get();
    if (applicationHost == null) {
      applicationHost = wingsPersistence.saveAndGet(Host.class, appHost);
    }
    return applicationHost;
  }

  @Override
  public boolean exist(String appId, String hostId) {
    return wingsPersistence.createQuery(Host.class).field(ID_KEY).equal(hostId).field("appId").equal(appId).getKey()
        != null;
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#importHosts(java.lang.String, java.lang.String,
   * software.wings.utils.BoundedInputStream)
   */
  @Override
  public int importHosts(String appId, String envId, String infraId, BoundedInputStream inputStream) {
    List<Host> hosts = csvFileHelper.parseHosts(infraId, appId, envId, inputStream);
    return (int) hosts.stream()
        .map(host -> {
          host.setEnvId(envId);
          return saveHost(host);
        })
        .filter(Objects::nonNull)
        .count();
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.HostService#getHostsByHostIds(java.lang.String, java.util.List)
   */
  @Override
  public List<Host> getHostsByHostIds(String appId, String envId, List<String> hostUuids) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    return wingsPersistence.createQuery(Host.class).field("appId").equal(appId).field("envId").equal(envId).asList();
  }

  @Override
  public Host getHostByEnv(String appId, String envId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .field(ID_KEY)
        .equal(hostId)
        .get();
  }

  @Override
  public void delete(String appId, String envId, String hostId) {
    Host applicationHost = get(appId, envId, hostId);
    if (delete(applicationHost)) {
      Environment environment = environmentService.get(applicationHost.getAppId(), applicationHost.getEnvId(), false);
      //      notificationService.sendNotificationAsync(
      //          anInformationNotification().withAppId(applicationHost.getAppId()).withNotificationTemplateId(HOST_DELETE_NOTIFICATION)
      //              .withNotificationTemplateVariables(ImmutableMap.of("HOST_NAME", applicationHost.getHostName(),
      //              "ENV_NAME", environment.getName())).build());
    }
  }

  private boolean delete(Host applicationHost) {
    if (applicationHost != null) {
      boolean delete = wingsPersistence.delete(applicationHost);
      if (delete) {
        executorService.submit(
            () -> configService.deleteByEntityId(applicationHost.getAppId(), applicationHost.getUuid()));
      }
      return delete;
    }
    return false;
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Host.class)
        .field("appId")
        .equal(appId)
        .field("envId")
        .equal(envId)
        .asList()
        .forEach(this ::delete);
  }

  @Override
  public void deleteByHostName(String appId, String infraMappingId, String hostName) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .field("appId")
                                .equal(appId)
                                .field("infraMappingId")
                                .equal(infraMappingId)
                                .field("hostName")
                                .equal(hostName));
  }

  @Override
  public void updateHostConnectionAttrByInfraMappingId(
      String appId, String infraMappingId, String hostConnectionAttrs) {
    Query<Host> query = wingsPersistence.createQuery(Host.class)
                            .field("appId")
                            .equal(appId)
                            .field("infraMappingId")
                            .equal(infraMappingId);
    UpdateOperations<Host> operations =
        wingsPersistence.createUpdateOperations(Host.class).set("hostConnAttr", hostConnectionAttrs);
    wingsPersistence.update(query, operations);
  }

  @Override
  public void deleteByInfraMappingId(String appId, String infraMappingId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .field("appId")
                                .equal(appId)
                                .field("infraMappingId")
                                .equal(infraMappingId));
  }
}
