package software.wings.service.impl;

import static java.time.Duration.ofSeconds;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.utils.Validator.notNullCheck;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.InvalidRequestException;
import software.wings.scheduler.PruneEntityJob;
import software.wings.scheduler.QuartzScheduler;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ownership.OwnedByHost;
import software.wings.utils.BoundedInputStream;
import software.wings.utils.HostCsvFileHelper;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/9/16.
 */
@ValidateOnExecution
@Singleton
public class HostServiceImpl implements HostService {
  private static final Logger logger = LoggerFactory.getLogger(HostServiceImpl.class);
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;
  @Inject private NotificationService notificationService;
  @Inject private EnvironmentService environmentService;
  @Inject private ConfigService configService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceInstanceService serviceInstanceService;

  @Inject @Named("JobScheduler") private QuartzScheduler jobScheduler;

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
                    .filter(ID_KEY, hostId)
                    .filter("envId", envId)
                    .filter("appId", appId)
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
      throw new InvalidRequestException("Host doesn't exist");
    }

    ImmutableMap.Builder builder = ImmutableMap.<String, Object>builder().put("hostConnAttr", host.getHostConnAttr());
    if (host.getBastionConnAttr() != null) {
      builder.put("bastionConnAttr", host.getBastionConnAttr());
    }
    wingsPersistence.updateFields(Host.class, savedHost.getUuid(), builder.build());

    return get(savedHost.getAppId(), savedHost.getEnvId(), host.getUuid());
  }

  @Override
  public Host saveHost(Host appHost) {
    Host applicationHost = wingsPersistence.createQuery(Host.class)
                               .filter("serviceTemplateId", appHost.getServiceTemplateId())
                               .filter("hostName", appHost.getHostName())
                               .filter("publicDns", appHost.getPublicDns())
                               .filter("appId", appHost.getAppId())
                               .filter("envId", appHost.getEnvId())
                               .get();
    return applicationHost != null ? applicationHost : wingsPersistence.saveAndGet(Host.class, appHost);
  }

  @Override
  public boolean exist(String appId, String hostId) {
    return wingsPersistence.createQuery(Host.class).filter(ID_KEY, hostId).filter("appId", appId).getKey() != null;
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
        .filter("appId", appId)
        .filter("envId", envId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    return wingsPersistence.createQuery(Host.class).filter("appId", appId).filter("envId", envId).asList();
  }

  @Override
  public Host getHostByEnv(String appId, String envId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter(ID_KEY, hostId)
        .get();
  }

  @Override
  public void delete(String appId, String envId, String hostId) {
    Host applicationHost = get(appId, envId, hostId);
    delete(applicationHost);
  }

  private boolean delete(Host host) {
    if (host == null) {
      return true;
    }

    PruneEntityJob.addDefaultJob(
        jobScheduler, Host.class, host.getAppId(), host.getUuid(), ofSeconds(5), ofSeconds(15));
    return wingsPersistence.delete(host);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String hostId) {
    List<OwnedByHost> services = ServiceClassLocator.descendingServices(this, HostServiceImpl.class, OwnedByHost.class);
    PruneEntityJob.pruneDescendingEntities(services, descending -> descending.pruneByHost(appId, hostId));
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .asList()
        .forEach(this ::delete);
  }

  @Override
  public void deleteByDnsName(String appId, String infraMappingId, String dnsName) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .filter("appId", appId)
                                .filter("infraMappingId", infraMappingId)
                                .filter("publicDns", dnsName));
  }

  @Override
  public void updateHostConnectionAttrByInfraMappingId(
      String appId, String infraMappingId, String hostConnectionAttrs) {
    Query<Host> query =
        wingsPersistence.createQuery(Host.class).filter("appId", appId).filter("infraMappingId", infraMappingId);
    UpdateOperations<Host> operations =
        wingsPersistence.createUpdateOperations(Host.class).set("hostConnAttr", hostConnectionAttrs);
    wingsPersistence.update(query, operations);
  }

  @Override
  public void pruneByInfrastructureMapping(String appId, String infraMappingId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Host.class).filter("appId", appId).filter("infraMappingId", infraMappingId));
  }

  @Override
  public void deleteByService(String appId, String envId, String serviceTemplateId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(Host.class).filter("appId", appId).filter("serviceTemplateId", serviceTemplateId));
  }
}
