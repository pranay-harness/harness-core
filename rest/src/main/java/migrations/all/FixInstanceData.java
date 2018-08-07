package migrations.all;

import static software.wings.dl.PageRequest.PageRequestBuilder.aPageRequest;
import static software.wings.exception.WingsException.ExecutionContext.MANAGER;
import static software.wings.service.impl.instance.InstanceHandler.AUTO_SCALE;

import com.google.inject.Inject;

import migrations.Migration;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.Account;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureMappingType;
import software.wings.beans.SearchFilter.Operator;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.infrastructure.instance.Instance;
import software.wings.beans.infrastructure.instance.info.AutoScalingGroupInstanceInfo;
import software.wings.beans.infrastructure.instance.info.CodeDeployInstanceInfo;
import software.wings.beans.infrastructure.instance.info.EcsContainerInfo;
import software.wings.beans.infrastructure.instance.info.InstanceInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.beans.infrastructure.instance.info.PcfInstanceInfo;
import software.wings.dl.PageRequest;
import software.wings.dl.PageResponse;
import software.wings.dl.WingsPersistence;
import software.wings.exception.WingsException;
import software.wings.exception.WingsExceptionMapper;
import software.wings.lock.AcquiredLock;
import software.wings.lock.PersistentLocker;
import software.wings.service.impl.instance.ContainerInstanceHandler;
import software.wings.service.impl.instance.InstanceHandler;
import software.wings.service.impl.instance.InstanceHandlerFactory;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.AppService;
import software.wings.service.intfc.InfrastructureMappingService;
import software.wings.utils.Util;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

/**
 * Migration script to fix instance data
 * @author rktummala on 05/03/18
 */
public class FixInstanceData implements Migration {
  private static final Logger logger = LoggerFactory.getLogger(FixInstanceData.class);

  @Inject private WingsPersistence wingsPersistence;
  @Inject private InstanceHandlerFactory instanceHandlerFactory;
  @Inject private AccountService accountService;
  @Inject private AppService appService;
  @Inject private InfrastructureMappingService infraMappingService;
  @Inject private PersistentLocker persistentLocker;
  @Inject private ContainerInstanceHandler containerInstanceHandler;

  @Override
  public void migrate() {
    PageRequest<Account> accountPageRequest = aPageRequest().addFieldsIncluded("_id").build();
    List<Account> accounts = accountService.list(accountPageRequest);
    accounts.forEach(account -> {
      List<String> appIds = appService.getAppIdsByAccountId(account.getUuid());
      appIds.forEach(appId -> {
        try {
          logger.info("Fixing instances for appId:" + appId);
          PageRequest<InfrastructureMapping> pageRequest = new PageRequest<>();
          pageRequest.addFilter("appId", Operator.EQ, appId);
          PageResponse<InfrastructureMapping> response = infraMappingService.list(pageRequest);
          // Response only contains id
          List<InfrastructureMapping> infraMappingList = response.getResponse();

          infraMappingList.forEach(infraMapping -> {
            String infraMappingId = infraMapping.getUuid();
            logger.info("Fixing instances for inframappingId:" + infraMappingId);
            InfrastructureMappingType infraMappingType =
                Util.getEnumFromString(InfrastructureMappingType.class, infraMapping.getInfraMappingType());
            try (AcquiredLock lock = persistentLocker.tryToAcquireLock(
                     InfrastructureMapping.class, infraMappingId, Duration.ofSeconds(120))) {
              if (lock == null) {
                return;
              }

              try {
                List<Instance> instances = wingsPersistence.createQuery(Instance.class)
                                               .field("infraMappingId")
                                               .equal(infraMappingId)
                                               .field("appId")
                                               .equal(appId)
                                               .asList();

                instances.forEach(instance -> {
                  // If these instances were identified at a later periodic sync
                  if (instance.getLastWorkflowExecutionId() == null) {
                    Optional<Instance> instanceOptional =
                        getInstanceFromPreviousDeployment(instances, infraMapping, instance);
                    if (instanceOptional.isPresent()) {
                      Instance instanceFromPreviousDeployment = instanceOptional.get();
                      InstanceHandler instanceHandler = instanceHandlerFactory.getInstanceHandler(infraMappingType);
                      if (instanceHandler == null) {
                        logger.error("No instance handler found for inframappingType {} for instance {}",
                            infraMappingType, instance.getUuid());
                        return;
                      }

                      instance.setAppId(instanceFromPreviousDeployment.getAppId());
                      instance.setAccountId(instanceFromPreviousDeployment.getAccountId());
                      instance.setInfraMappingId(instanceFromPreviousDeployment.getInfraMappingId());
                      instance.setInfraMappingId(instanceFromPreviousDeployment.getInfraMappingId());
                      instance.setLastWorkflowExecutionId(instanceFromPreviousDeployment.getLastWorkflowExecutionId());
                      instance.setLastWorkflowExecutionName(
                          instanceFromPreviousDeployment.getLastWorkflowExecutionName());

                      instance.setLastArtifactId(instanceFromPreviousDeployment.getLastArtifactId());
                      instance.setLastArtifactName(instanceFromPreviousDeployment.getLastArtifactName());
                      instance.setLastArtifactStreamId(instanceFromPreviousDeployment.getLastArtifactStreamId());
                      instance.setLastArtifactSourceName(instanceFromPreviousDeployment.getLastArtifactSourceName());
                      instance.setLastArtifactBuildNum(instanceFromPreviousDeployment.getLastArtifactBuildNum());

                      instance.setLastPipelineExecutionId(instanceFromPreviousDeployment.getLastPipelineExecutionId());
                      instance.setLastPipelineExecutionName(
                          instanceFromPreviousDeployment.getLastPipelineExecutionName());

                      // Commented this out, so we can distinguish between autoscales instances and instances we
                      // deployed
                      instance.setLastDeployedById(AUTO_SCALE);
                      instance.setLastDeployedByName(AUTO_SCALE);
                      //                      instance.setLastDeployedAt(System.currentTimeMillis());

                      wingsPersistence.merge(instance);

                    } else {
                      logger.error("No previous deployment was found for id {} in inframappingId {}",
                          instance.getUuid(), infraMappingId);
                      return;
                    }
                  }

                  if (instance.getLastDeployedAt() == 0) {
                    WorkflowExecution workflowExecution = wingsPersistence.createQuery(WorkflowExecution.class)
                                                              .filter("_id", instance.getLastWorkflowExecutionId())
                                                              .filter("appId", appId)
                                                              .project("endTs", true)
                                                              .get();
                    if (workflowExecution != null) {
                      Long endTs = workflowExecution.getEndTs();
                      wingsPersistence.updateField(
                          Instance.class, instance.getUuid(), "lastDeployedAt", endTs != null ? endTs.longValue() : 0);
                    }
                  }
                });
                logger.info("Instance fix completed for [{}]", infraMappingId);
              } catch (Exception ex) {
                logger.warn("Instance fix failed for infraMappingId [{}]", infraMappingId, ex);
              }
            } catch (Exception e) {
              logger.warn("Failed to acquire lock for infraMappingId [{}] of appId [{}]", infraMappingId, appId);
            }
          });

          logger.info("Instance sync done for appId:" + appId);
        } catch (WingsException exception) {
          WingsExceptionMapper.logProcessedMessages(exception, MANAGER, logger);
        } catch (Exception ex) {
          logger.warn("Error while syncing instances for app: {}", appId, ex);
        }
      });
    });
  }

  private Optional<Instance> getInstanceFromPreviousDeployment(
      List<Instance> instanceList, InfrastructureMapping infraMapping, Instance instanceToBeFixed) {
    return instanceList.stream()
        .filter(instance -> {
          if (instance.getLastWorkflowExecutionId() == null) {
            return false;
          }

          String lhs;
          String rhs;
          InstanceInfo lhsInstanceInfo = instanceToBeFixed.getInstanceInfo();
          InstanceInfo rhsInstanceInfo = instance.getInstanceInfo();
          String infraMappingType = infraMapping.getInfraMappingType();
          if (containerInstanceHandler.isContainerDeployment(infraMapping)) {
            if (lhsInstanceInfo instanceof KubernetesContainerInfo) {
              lhs = ((KubernetesContainerInfo) lhsInstanceInfo).getControllerName();
              rhs = ((KubernetesContainerInfo) rhsInstanceInfo).getControllerName();

            } else if (lhsInstanceInfo instanceof EcsContainerInfo) {
              lhs = ((EcsContainerInfo) lhsInstanceInfo).getTaskDefinitionArn();
              rhs = ((EcsContainerInfo) rhsInstanceInfo).getTaskDefinitionArn();
            } else {
              logger.error("InframappingType {} is not supported", infraMappingType);
              return false;
            }
          } else if (InfrastructureMappingType.PCF_PCF.name().equals(infraMappingType)) {
            lhs = ((PcfInstanceInfo) lhsInstanceInfo).getPcfApplicationGuid();
            rhs = ((PcfInstanceInfo) rhsInstanceInfo).getPcfApplicationGuid();
          } else if (InfrastructureMappingType.AWS_AMI.name().equals(infraMappingType)) {
            lhs = ((AutoScalingGroupInstanceInfo) lhsInstanceInfo).getAutoScalingGroupName();
            rhs = ((AutoScalingGroupInstanceInfo) rhsInstanceInfo).getAutoScalingGroupName();
          } else if (InfrastructureMappingType.AWS_AWS_CODEDEPLOY.name().equals(infraMappingType)) {
            lhs = ((CodeDeployInstanceInfo) lhsInstanceInfo).getDeploymentId();
            rhs = ((CodeDeployInstanceInfo) rhsInstanceInfo).getDeploymentId();
          } else {
            logger.error("InframappingType {} is not supported", infraMappingType);
            return false;
          }

          return StringUtils.equals(lhs, rhs);
        })
        .findFirst();
  }
}
