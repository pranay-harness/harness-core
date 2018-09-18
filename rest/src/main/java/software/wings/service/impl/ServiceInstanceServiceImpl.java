package software.wings.service.impl;

import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.mongodb.morphia.mapping.Mapper.ID_KEY;
import static software.wings.beans.Base.APP_ID_KEY;
import static software.wings.beans.ServiceInstance.Builder.aServiceInstance;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.beans.PageRequest;
import io.harness.beans.PageResponse;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.Activity;
import software.wings.beans.Activity.Type;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.ServiceInstance;
import software.wings.beans.ServiceTemplate;
import software.wings.beans.infrastructure.Host;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.ServiceInstanceService;

import java.util.List;
import java.util.Set;
import javax.validation.executable.ValidateOnExecution;

/**
 * Created by anubhaw on 5/26/16.
 */
@ValidateOnExecution
@Singleton
public class ServiceInstanceServiceImpl implements ServiceInstanceService {
  @Inject private WingsPersistence wingsPersistence;

  @Override
  public PageResponse<ServiceInstance> list(PageRequest<ServiceInstance> pageRequest) {
    return wingsPersistence.query(ServiceInstance.class, pageRequest);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#save(software.wings.beans.ServiceInstance)
   */
  @Override
  public ServiceInstance save(ServiceInstance serviceInstance) {
    return wingsPersistence.saveAndGet(ServiceInstance.class, serviceInstance);
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#get(java.lang.String, java.lang.String, java.lang.String)
   */
  @Override
  public ServiceInstance get(String appId, String envId, String instanceId) {
    return wingsPersistence.createQuery(ServiceInstance.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter(ID_KEY, instanceId)
        .get();
  }

  /* (non-Javadoc)
   * @see
   * software.wings.service.intfc.ServiceInstanceService#updateInstanceMappings(software.wings.beans.ServiceTemplate,
   * java.util.List, java.util.List)
   */
  @Override
  public List<ServiceInstance> updateInstanceMappings(
      ServiceTemplate template, InfrastructureMapping infraMapping, List<Host> hosts) {
    return hosts.stream()
        .map(host -> {
          ServiceInstance serviceInstance = wingsPersistence.createQuery(ServiceInstance.class)
                                                .filter("infraMappingId", infraMapping.getUuid())
                                                .filter("hostId", host.getUuid())
                                                .filter("hostName", host.getHostName())
                                                .filter("publicDns", host.getPublicDns())
                                                .get();
          return serviceInstance != null ? serviceInstance
                                         : wingsPersistence.saveAndGet(ServiceInstance.class,
                                               aServiceInstance()
                                                   .withAppId(template.getAppId())
                                                   .withEnvId(template.getEnvId())
                                                   .withServiceTemplate(template)
                                                   .withHost(host)
                                                   .withInfraMappingId(infraMapping.getUuid())
                                                   .withInfraMappingType(infraMapping.getComputeProviderType())
                                                   .build());
        })
        .collect(toList());
  }

  /* (non-Javadoc)
   * @see software.wings.service.intfc.ServiceInstanceService#delete(java.lang.String, java.lang.String,
   * java.lang.String)
   */
  @Override
  public void delete(String appId, String envId, String instanceId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceInstance.class)
                                .filter("appId", appId)
                                .filter("envId", envId)
                                .filter(ID_KEY, instanceId));
  }

  @Override
  public void deleteByEnv(String appId, String envId) {
    wingsPersistence.createQuery(ServiceInstance.class)
        .filter(ServiceInstance.APP_ID_KEY, appId)
        .filter("envId", envId)
        .asList()
        .forEach(serviceInstance -> delete(appId, envId, serviceInstance.getUuid()));
  }

  @Override
  public void deleteByServiceTemplate(String appId, String envId, String templateId) {
    wingsPersistence.createQuery(ServiceInstance.class)
        .filter("appId", appId)
        .filter("envId", envId)
        .filter("serviceTemplate", templateId)
        .asList()
        .forEach(serviceInstance -> delete(appId, envId, serviceInstance.getUuid()));
  }

  @Override
  public void updateActivity(Activity activity) {
    Query<ServiceInstance> query = wingsPersistence.createQuery(ServiceInstance.class)
                                       .filter("appId", activity.getAppId())
                                       .filter(ID_KEY, activity.getServiceInstanceId());
    UpdateOperations<ServiceInstance> operations = wingsPersistence.createUpdateOperations(ServiceInstance.class);

    if (isNotBlank(activity.getArtifactId())) {
      operations.set("artifactId", activity.getArtifactId())
          .set("artifactName", activity.getArtifactName())
          .set("artifactStreamId", activity.getArtifactStreamId())
          .set("artifactStreamName", activity.getArtifactStreamName())
          .set("artifactDeployedOn", activity.getCreatedAt())
          .set("artifactDeploymentStatus", activity.getStatus())
          .set("artifactDeploymentActivityId", activity.getUuid());
    }
    operations.set("lastActivityId", activity.getUuid())
        .set("lastActivityStatus", activity.getStatus())
        .set("commandName", activity.getCommandName())
        .set("commandType", activity.getCommandType())
        .set("lastActivityCreatedAt", activity.getCreatedAt());

    if (activity.getType() == Type.Command && isNotBlank(activity.getArtifactId())) {
      operations.set("lastDeployedOn", activity.getLastUpdatedAt());
    }
    wingsPersistence.update(query, operations);
  }

  @Override
  public void pruneByInfrastructureMapping(String appId, String infraMappingId) {
    wingsPersistence.delete(wingsPersistence.createQuery(ServiceInstance.class)
                                .filter("appId", appId)
                                .filter("infraMappingId", infraMappingId));
  }

  @Override
  public void pruneByHost(String appId, String hostId) {
    wingsPersistence.delete(
        wingsPersistence.createQuery(ServiceInstance.class).filter("appId", appId).filter("hostId", hostId));
  }

  @Override
  public List<ServiceInstance> fetchServiceInstances(String appId, Set<String> uuids) {
    return wingsPersistence.createQuery(ServiceInstance.class)
        .filter(APP_ID_KEY, appId)
        .field(ID_KEY)
        .in(uuids)
        .asList();
  }
}
