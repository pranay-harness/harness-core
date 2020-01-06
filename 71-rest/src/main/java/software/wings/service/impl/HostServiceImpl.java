package software.wings.service.impl;

import static io.harness.validation.Validator.notNullCheck;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import io.harness.exception.InvalidRequestException;
import io.harness.queue.QueuePublisher;
import io.harness.stream.BoundedInputStream;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.api.DeploymentType;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.infrastructure.Host;
import software.wings.beans.infrastructure.Host.HostKeys;
import software.wings.dl.WingsPersistence;
import software.wings.prune.PruneEntityListener;
import software.wings.prune.PruneEvent;
import software.wings.service.intfc.ConfigService;
import software.wings.service.intfc.EnvironmentService;
import software.wings.service.intfc.HostService;
import software.wings.service.intfc.NotificationService;
import software.wings.service.intfc.ServiceInstanceService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.ownership.OwnedByHost;
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
@Slf4j
public class HostServiceImpl implements HostService {
  @Inject private WingsPersistence wingsPersistence;
  @Inject private HostCsvFileHelper csvFileHelper;
  @Inject private NotificationService notificationService;
  @Inject private EnvironmentService environmentService;
  @Inject private ConfigService configService;
  @Inject private ExecutorService executorService;
  @Inject private ServiceInstanceService serviceInstanceService;
  @Inject private ServiceResourceService serviceResourceService;

  @Inject private QueuePublisher<PruneEvent> pruneQueue;

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
                    .filter(HostKeys.envId, envId)
                    .filter("appId", appId)
                    .get();
    notNullCheck("Host is null for hostId: " + hostId + " appId: " + appId + " envId: " + envId, host);
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
    if (host.getProperties() != null) {
      builder.put("properties", host.getProperties());
    }
    wingsPersistence.updateFields(Host.class, savedHost.getUuid(), builder.build());

    return get(savedHost.getAppId(), savedHost.getEnvId(), host.getUuid());
  }

  @Override
  public Host saveHost(Host appHost) {
    Host applicationHost = wingsPersistence.createQuery(Host.class)
                               .filter(HostKeys.serviceTemplateId, appHost.getServiceTemplateId())
                               .filter(HostKeys.hostName, appHost.getHostName())
                               .filter(HostKeys.publicDns, appHost.getPublicDns())
                               .filter("appId", appHost.getAppId())
                               .filter(HostKeys.envId, appHost.getEnvId())
                               .filter(HostKeys.infraMappingId, appHost.getInfraMappingId())
                               .filter(HostKeys.properties, appHost.getProperties())
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
        .filter(HostKeys.envId, envId)
        .field(ID_KEY)
        .hasAnyOf(hostUuids)
        .asList();
  }

  @Override
  public List<Host> getHostsByEnv(String appId, String envId) {
    return wingsPersistence.createQuery(Host.class).filter("appId", appId).filter(HostKeys.envId, envId).asList();
  }

  @Override
  public List<Host> getHostsByInfraMappingIds(String appId, List<String> infraMappingIds) {
    return wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .field(HostKeys.infraMappingId)
        .in(infraMappingIds)
        .asList();
  }

  @Override
  public List<Host> getHostsByInfraDefinitionIds(String appId, List<String> infraDefinitionIds) {
    return wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .field(HostKeys.infraDefinitionId)
        .in(infraDefinitionIds)
        .asList();
  }

  @Override
  public Host getHostByEnv(String appId, String envId, String hostId) {
    return wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .filter(HostKeys.envId, envId)
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
    pruneQueue.send(new PruneEvent(Host.class, host.getAppId(), host.getUuid()));
    return wingsPersistence.delete(host);
  }

  @Override
  public void pruneDescendingEntities(@NotEmpty String appId, @NotEmpty String hostId) {
    List<OwnedByHost> services = ServiceClassLocator.descendingServices(this, HostServiceImpl.class, OwnedByHost.class);
    PruneEntityListener.pruneDescendingEntities(services, descending -> descending.pruneByHost(appId, hostId));
  }

  @Override
  public void deleteByEnvironment(String appId, String envId) {
    wingsPersistence.createQuery(Host.class)
        .filter("appId", appId)
        .filter(HostKeys.envId, envId)
        .asList()
        .forEach(this ::delete);
  }

  @Override
  public void deleteByDnsName(String appId, String infraMappingId, String dnsName) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .filter("appId", appId)
                                .filter(HostKeys.infraMappingId, infraMappingId)
                                .filter(HostKeys.publicDns, dnsName));
  }

  @Override
  public void updateHostConnectionAttrByInfraMapping(
      InfrastructureMapping infrastructureMapping, String hostConnectionAttrs) {
    String appId = infrastructureMapping.getAppId();
    String infraMappingId = infrastructureMapping.getUuid();
    DeploymentType deploymentType =
        serviceResourceService.getDeploymentType(infrastructureMapping, null, infrastructureMapping.getServiceId());

    Query<Host> query =
        wingsPersistence.createQuery(Host.class).filter("appId", appId).filter(HostKeys.infraMappingId, infraMappingId);

    UpdateOperations<Host> operations = DeploymentType.SSH == deploymentType
        ? wingsPersistence.createUpdateOperations(Host.class).set("hostConnAttr", hostConnectionAttrs)
        : wingsPersistence.createUpdateOperations(Host.class).set("winrmConnAttr", hostConnectionAttrs);
    wingsPersistence.update(query, operations);
  }

  @Override
  public void pruneByInfrastructureMapping(String appId, String infraMappingId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .filter("appId", appId)
                                .filter(HostKeys.infraMappingId, infraMappingId));
  }

  @Override
  public void deleteByService(String appId, String envId, String serviceTemplateId) {
    wingsPersistence.delete(wingsPersistence.createQuery(Host.class)
                                .filter("appId", appId)
                                .filter(HostKeys.serviceTemplateId, serviceTemplateId));
  }
}
