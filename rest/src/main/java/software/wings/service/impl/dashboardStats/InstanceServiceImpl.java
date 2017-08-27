package software.wings.service.impl.dashboardStats;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.beans.infrastructure.Instance;
import software.wings.beans.infrastructure.InstanceKey;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.ArtifactService;
import software.wings.service.intfc.ArtifactStreamService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.service.intfc.ServiceResourceService;
import software.wings.service.intfc.dashboardStats.InstanceService;
import software.wings.utils.Validator;

import java.util.List;
import java.util.concurrent.ExecutorService;
import javax.validation.Valid;

/**
 * @author rktummala on 8/13/17
 */
@Singleton
public class InstanceServiceImpl implements InstanceService {
  private static final Logger logger = LoggerFactory.getLogger(InstanceServiceImpl.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private ArtifactStreamService artifactStreamService;
  @Inject private ArtifactService artifactService;
  @Inject private InfrastructureMappingService infrastructureMappingService;
  @Inject private AppService appService;
  @Inject private ServiceResourceService serviceResourceService;
  @Inject private ExecutorService executorService;

  @Override
  public Instance save(Instance instance) {
    if (logger.isDebugEnabled()) {
      logger.debug("Begin - Instance save called for hostName:" + instance.getHostName()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }
    if (!appService.exist(instance.getAppId())) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT);
    }

    // TODO check with Anubhaw if its is a overkill to validate all these entities since each of them would result in a
    // db call.

    //    ArtifactStream artifactStream = artifactStreamService.get(instance.getAppId(),
    //    instance.getLastArtifactStreamId()); Validator.notNullCheck("Artifact Stream", artifactStream);
    //
    //    InfrastructureMapping infrastructureMapping = infrastructureMappingService.get(instance.getAppId(),
    //    instance.getInfraMappingId()); Validator.notNullCheck("Infra Mapping", infrastructureMapping);
    //
    //    Artifact artifact = artifactService.get(instance.getAppId(), instance.getLastArtifactId());
    //    Validator.notNullCheck("Artifact", artifact);
    //
    //    Service service = serviceResourceService.get(instance.getAppId(), instance.getServiceId());
    //    Validator.notNullCheck("Service", service);

    Instance currentInstance = get(instance.getAppId(), instance.getUuid());
    Validator.nullCheck("Instance", currentInstance);

    String key = wingsPersistence.save(instance);
    Instance updatedInstance = wingsPersistence.get(Instance.class, instance.getAppId(), key);
    if (logger.isDebugEnabled()) {
      logger.debug("End - Instance save called for hostName:" + instance.getHostName()
          + " and infraMappingId:" + instance.getInfraMappingId());
    }

    return updatedInstance;
  }

  @Override
  public List<Instance> saveOrUpdate(List<Instance> instances) {
    List<Instance> updatedInstanceList = Lists.newArrayList();
    for (Instance instance : instances) {
      Instance updatedInstance = saveOrUpdate(instance);
      if (updatedInstance != null) {
        updatedInstanceList.add(updatedInstance);
      }
    }
    return updatedInstanceList;
  }

  @Override
  public Instance get(String appId, String instanceId) {
    return wingsPersistence.get(Instance.class, appId, instanceId);
  }

  @Override
  public Instance saveOrUpdate(@Valid Instance instance) {
    InstanceKey instanceKey = InstanceKey.Builder.anInstanceKey()
                                  .withHostName(instance.getHostName())
                                  .withInfraMappingId(instance.getInfraMappingId())
                                  .build();
    synchronized (instanceKey) {
      Instance existingInstance = wingsPersistence.createQuery(Instance.class)
                                      .field("hostName")
                                      .equal(instance.getHostName())
                                      .field("infraMappingId")
                                      .equal(instance.getInfraMappingId())
                                      .get();
      if (existingInstance == null) {
        return save(instance);
      } else {
        instance.setUuid(existingInstance.getUuid());
        String uuid = wingsPersistence.merge(instance);
        return wingsPersistence.get(Instance.class, uuid);
      }
    }
  }

  @Override
  public boolean delete(String appId, String instanceId) {
    Instance instance = get(appId, instanceId);
    Validator.notNullCheck("Instance", instance);
    return wingsPersistence.delete(instance);
  }
}
