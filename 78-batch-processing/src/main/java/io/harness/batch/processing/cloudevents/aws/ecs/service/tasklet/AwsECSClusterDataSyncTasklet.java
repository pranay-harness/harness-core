package io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet;

import static io.harness.batch.processing.ccm.UtilizationInstanceType.ECS_CLUSTER;
import static io.harness.batch.processing.ccm.UtilizationInstanceType.ECS_SERVICE;
import static java.util.function.Function.identity;
import static software.wings.beans.SettingAttribute.SettingCategory.CE_CONNECTOR;
import static software.wings.settings.SettingVariableTypes.CE_AWS;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.google.common.collect.Sets.SetView;
import com.google.inject.Singleton;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Attribute;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.ContainerInstanceStatus;
import com.amazonaws.services.ecs.model.DesiredStatus;
import com.amazonaws.services.ecs.model.LaunchType;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import io.harness.batch.processing.billing.timeseries.data.InstanceUtilizationData;
import io.harness.batch.processing.billing.timeseries.service.impl.UtilizationDataServiceImpl;
import io.harness.batch.processing.ccm.CCMJobConstants;
import io.harness.batch.processing.ccm.ClusterType;
import io.harness.batch.processing.ccm.InstanceCategory;
import io.harness.batch.processing.ccm.InstanceState;
import io.harness.batch.processing.ccm.InstanceType;
import io.harness.batch.processing.ccm.Resource;
import io.harness.batch.processing.cloudevents.aws.ecs.service.CEClusterDao;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsEC2HelperService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.support.intfc.AwsECSHelperService;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.EcsMetricClient;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.EcsUtilizationData;
import io.harness.batch.processing.cloudevents.aws.ecs.service.tasklet.support.response.MetricValue;
import io.harness.batch.processing.dao.intfc.InstanceDataDao;
import io.harness.batch.processing.entities.InstanceData;
import io.harness.batch.processing.pricing.data.CloudProvider;
import io.harness.batch.processing.service.intfc.InstanceDataService;
import io.harness.batch.processing.service.intfc.InstanceResourceService;
import io.harness.batch.processing.tasklet.util.InstanceMetaDataUtils;
import io.harness.batch.processing.writer.constants.InstanceMetaDataConstants;
import io.harness.ccm.health.LastReceivedPublishedMessageDao;
import io.harness.ccm.setup.CECloudAccountDao;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.StepContribution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.CollectionUtils;
import software.wings.api.ContainerDeploymentInfoWithNames;
import software.wings.api.DeploymentSummary;
import software.wings.beans.AwsCrossAccountAttributes;
import software.wings.beans.SettingAttribute;
import software.wings.beans.ce.CEAwsConfig;
import software.wings.beans.ce.CECloudAccount;
import software.wings.beans.ce.CECluster;
import software.wings.beans.infrastructure.instance.key.deployment.ContainerDeploymentKey;
import software.wings.beans.instance.HarnessServiceInfo;
import software.wings.service.intfc.instance.CloudToHarnessMappingService;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class AwsECSClusterDataSyncTasklet implements Tasklet {
  @Autowired private CEClusterDao ceClusterDao;
  @Autowired private EcsMetricClient ecsMetricClient;
  @Autowired private InstanceDataDao instanceDataDao;
  @Autowired private CECloudAccountDao ceCloudAccountDao;
  @Autowired private AwsECSHelperService awsECSHelperService;
  @Autowired private AwsEC2HelperService awsEC2HelperService;
  @Autowired private InstanceDataService instanceDataService;
  @Autowired private UtilizationDataServiceImpl utilizationDataService;
  @Autowired private InstanceResourceService instanceResourceService;
  @Autowired private CloudToHarnessMappingService cloudToHarnessMappingService;
  @Autowired private LastReceivedPublishedMessageDao lastReceivedPublishedMessageDao;
  private JobParameters parameters;

  private static final String ECS_OS_TYPE = "ecs.os-type";

  @Override
  public RepeatStatus execute(StepContribution stepContribution, ChunkContext chunkContext) throws Exception {
    parameters = chunkContext.getStepContext().getStepExecution().getJobParameters();
    String accountId = parameters.getString(CCMJobConstants.ACCOUNT_ID);
    List<CECluster> ceClusters = ceClusterDao.getCECluster(accountId);
    Map<String, AwsCrossAccountAttributes> crossAccountAttributes = getCrossAccountAttributes(accountId);
    ceClusters.forEach(ceCluster -> {
      AwsCrossAccountAttributes awsCrossAccountAttributes = crossAccountAttributes.get(ceCluster.getInfraAccountId());
      if (null != awsCrossAccountAttributes) {
        logger.info("Sync for cluster {}", ceCluster.getUuid());
        syncECSClusterData(accountId, awsCrossAccountAttributes, ceCluster);
        lastReceivedPublishedMessageDao.upsert(accountId, ceCluster.getHash());
      }
    });
    return null;
  }

  private void syncECSClusterData(
      String accountId, AwsCrossAccountAttributes awsCrossAccountAttributes, CECluster ceCluster) {
    List<ContainerInstance> containerInstances = listContainerInstances(awsCrossAccountAttributes, ceCluster);
    logger.debug("cluster {} Container instances {}", containerInstances, ceCluster);

    updateContainerInstance(accountId, ceCluster, awsCrossAccountAttributes, containerInstances);

    Map<String, String> taskArnServiceNameMap = new HashMap<>();
    loadTaskArnServiceNameMap(
        awsCrossAccountAttributes, ceCluster.getClusterArn(), ceCluster.getRegion(), taskArnServiceNameMap);
    List<Task> tasks = listTask(awsCrossAccountAttributes, ceCluster.getClusterArn(), ceCluster.getRegion());
    logger.debug("Task list {}", tasks);
    updateTasks(accountId, ceCluster, tasks, taskArnServiceNameMap);
    publishUtilizationMetrics(awsCrossAccountAttributes, ceCluster);
  }

  @VisibleForTesting
  void publishUtilizationMetrics(AwsCrossAccountAttributes awsCrossAccountAttributes, CECluster ceCluster) {
    Instant now = Instant.now().truncatedTo(ChronoUnit.HOURS);
    String clusterName = ceCluster.getClusterName();
    List<Service> services = listServices(awsCrossAccountAttributes, clusterName, ceCluster.getRegion());
    Cluster cluster = new Cluster().withClusterName(clusterName).withClusterArn(ceCluster.getClusterArn());

    List<EcsUtilizationData> utilizationMetrics =
        ecsMetricClient.getUtilizationMetrics(awsCrossAccountAttributes, Date.from(now.minus(2, ChronoUnit.HOURS)),
            Date.from(now.minus(1, ChronoUnit.HOURS)), cluster, services, ceCluster);
    updateUtilData(ceCluster, utilizationMetrics);
  }

  private void updateUtilData(CECluster ceCluster, List<EcsUtilizationData> utilizationMetricsList) {
    List<InstanceUtilizationData> instanceUtilizationDataList = new ArrayList<>();
    String accountId = ceCluster.getAccountId();
    utilizationMetricsList.forEach(utilizationMetrics -> {
      String serviceArn = utilizationMetrics.getServiceArn();
      String clusterId = utilizationMetrics.getClusterId();
      String settingId = utilizationMetrics.getSettingId();
      String instanceId;
      String instanceType;
      if (null == serviceArn) {
        instanceId = utilizationMetrics.getClusterArn();
        instanceType = ECS_CLUSTER;
      } else {
        instanceId = serviceArn;
        instanceType = ECS_SERVICE;
      }
      // Initialising List of Metrics to handle Utilization Metrics Downtime (Ideally this will be of size 1)
      // We do not need a Default value as such a scenario will never exist, if there is no data. It will not be
      // inserted to DB.
      List<Double> cpuUtilizationAvgList = new ArrayList<>();
      List<Double> cpuUtilizationMaxList = new ArrayList<>();
      List<Double> memoryUtilizationAvgList = new ArrayList<>();
      List<Double> memoryUtilizationMaxList = new ArrayList<>();
      List<Date> startTimestampList = new ArrayList<>();
      int metricsListSize = 0;

      for (MetricValue utilizationMetric : utilizationMetrics.getMetricValues()) {
        // Assumption that size of all the metrics and timestamps will be same across the 4 metrics
        startTimestampList = utilizationMetric.getTimestamps();
        List<Double> metricsList = utilizationMetric.getValues();
        metricsListSize = metricsList.size();

        switch (utilizationMetric.getStatistic()) {
          case "Maximum":
            switch (utilizationMetric.getMetricName()) {
              case "MemoryUtilization":
                memoryUtilizationMaxList = metricsList;
                break;
              case "CPUUtilization":
                cpuUtilizationMaxList = metricsList;
                break;
              default:
                throw new InvalidRequestException("Invalid Utilization metric name");
            }
            break;
          case "Average":
            switch (utilizationMetric.getMetricName()) {
              case "MemoryUtilization":
                memoryUtilizationAvgList = metricsList;
                break;
              case "CPUUtilization":
                cpuUtilizationAvgList = metricsList;
                break;
              default:
                throw new InvalidRequestException("Invalid Utilization metric name");
            }
            break;
          default:
            throw new InvalidRequestException("Invalid Utilization metric Statistic");
        }
      }

      // POJO and insertion to DB
      for (int metricIndex = 0; metricIndex < metricsListSize; metricIndex++) {
        long startTime = startTimestampList.get(metricIndex).toInstant().toEpochMilli();
        long oneHourMillis = Duration.ofHours(1).toMillis();

        InstanceUtilizationData utilizationData =
            InstanceUtilizationData.builder()
                .accountId(accountId)
                .instanceId(instanceId)
                .instanceType(instanceType)
                .clusterId(clusterId)
                .settingId(settingId)
                .cpuUtilizationMax(getScaledUtilValue(cpuUtilizationMaxList.get(metricIndex)))
                .cpuUtilizationAvg(getScaledUtilValue(cpuUtilizationAvgList.get(metricIndex)))
                .memoryUtilizationMax(getScaledUtilValue(memoryUtilizationMaxList.get(metricIndex)))
                .memoryUtilizationAvg(getScaledUtilValue(memoryUtilizationAvgList.get(metricIndex)))
                .startTimestamp(startTime)
                .endTimestamp(startTime + oneHourMillis)
                .build();

        instanceUtilizationDataList.add(utilizationData);
      }
    });

    utilizationDataService.create(instanceUtilizationDataList);
  }

  private double getScaledUtilValue(double value) {
    return value / 100;
  }

  private void updateTasks(
      String accountId, CECluster ceCluster, List<Task> tasks, Map<String, String> taskArnServiceNameMap) {
    Instant stopTime = Instant.now();
    String clusterId = ceCluster.getHash();
    String settingId = ceCluster.getParentAccountSettingId();
    String region = ceCluster.getRegion();
    Map<String, InstanceData> activeInstanceDataMap = getInstanceDataMap(accountId, clusterId,
        ImmutableList.of(InstanceType.ECS_TASK_FARGATE, InstanceType.ECS_TASK_EC2), InstanceState.RUNNING);
    Set<String> activeTaskIds = activeInstanceDataMap.keySet();
    Set<String> activeTaskArns =
        tasks.stream().map(task -> getIdFromArn(task.getTaskArn())).collect(Collectors.toSet());
    SetView<String> inactiveTaskArns = Sets.difference(activeTaskIds, activeTaskArns);
    activeInstanceDataMap.values()
        .stream()
        .filter(instanceData -> inactiveTaskArns.contains(instanceData.getInstanceId()))
        .forEach(instanceData -> instanceDataDao.updateInstanceStopTime(instanceData, stopTime));

    Set<String> containerInstanceArn = tasks.stream()
                                           .filter(task -> null != task.getContainerInstanceArn())
                                           .map(task -> getIdFromArn(task.getContainerInstanceArn()))
                                           .collect(Collectors.toSet());
    Map<String, InstanceData> instanceDataMap = getInstanceDataMap(containerInstanceArn);
    tasks.forEach(task -> {
      String taskId = getIdFromArn(task.getTaskArn());
      if (null != activeInstanceDataMap.get(taskId)) {
        InstanceData instanceData = activeInstanceDataMap.get(taskId);
        boolean updated = updateInstanceStopTimeForTask(instanceData, task);
        if (updated) {
          instanceDataDao.create(instanceData);
        }
      } else {
        String clusterName = getIdFromArn(task.getClusterArn());
        InstanceType instanceType = getInstanceType(task);
        double memory = Integer.parseInt(task.getMemory());
        double cpu = Integer.parseInt(task.getCpu());
        Resource resource = Resource.builder().cpuUnits(cpu).memoryMb(memory).build();
        Map<String, String> metaData = new HashMap<>();
        InstanceData containerInstantData = null;
        if (null != task.getContainerInstanceArn()) {
          containerInstantData = instanceDataMap.get(getIdFromArn(task.getContainerInstanceArn()));
        }
        if (InstanceType.ECS_TASK_EC2 == instanceType && null != containerInstantData) {
          String containerInstanceId = getIdFromArn(task.getContainerInstanceArn());

          metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY,
              containerInstantData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_FAMILY));
          metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY,
              containerInstantData.getMetaData().get(InstanceMetaDataConstants.INSTANCE_CATEGORY));
          metaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM,
              containerInstantData.getMetaData().get(InstanceMetaDataConstants.OPERATING_SYSTEM));
          metaData.put(InstanceMetaDataConstants.CONTAINER_INSTANCE_ARN, containerInstanceId);
          metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_CPU,
              String.valueOf(containerInstantData.getTotalResource().getCpuUnits()));
          metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_MEMORY,
              String.valueOf(containerInstantData.getTotalResource().getMemoryMb()));
          metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, containerInstanceId);
          metaData.put(InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, containerInstanceId);
        }
        metaData.put(InstanceMetaDataConstants.TASK_ID, taskId);
        metaData.put(InstanceMetaDataConstants.REGION, region);
        metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
        metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
        metaData.put(InstanceMetaDataConstants.LAUNCH_TYPE, task.getLaunchType());
        HarnessServiceInfo harnessServiceInfo = null;
        if (null != taskArnServiceNameMap.get(task.getTaskArn())) {
          String serviceArn = taskArnServiceNameMap.get(task.getTaskArn());
          String serviceName = getIdFromArn(serviceArn);
          metaData.put(InstanceMetaDataConstants.ECS_SERVICE_NAME, serviceName);
          metaData.put(InstanceMetaDataConstants.ECS_SERVICE_ARN, serviceArn);
          harnessServiceInfo = getHarnessServiceInfo(accountId, clusterName, serviceName);
        }

        InstanceData instanceData = InstanceData.builder()
                                        .accountId(accountId)
                                        .instanceId(taskId)
                                        .clusterName(clusterName)
                                        .clusterId(clusterId)
                                        .settingId(settingId)
                                        .instanceType(instanceType)
                                        .usageStartTime(task.getPullStartedAt().toInstant())
                                        .instanceState(InstanceState.RUNNING)
                                        .totalResource(resource)
                                        .allocatableResource(resource)
                                        .metaData(metaData)
                                        .harnessServiceInfo(harnessServiceInfo)
                                        .build();

        updateInstanceStopTimeForTask(instanceData, task);
        logger.debug("Creating task {} ", taskId);
        instanceDataService.create(instanceData);
      }
    });
  }

  private boolean updateInstanceStopTimeForTask(InstanceData instanceData, Task task) {
    if (null != task.getStoppedAt()) {
      instanceData.setUsageStopTime(task.getStoppedAt().toInstant());
      instanceData.setInstanceState(InstanceState.STOPPED);
      return true;
    }
    return false;
  }

  private HarnessServiceInfo getHarnessServiceInfo(String accountId, String clusterName, String serviceName) {
    ContainerDeploymentKey containerDeploymentKey =
        ContainerDeploymentKey.builder().containerServiceName(serviceName).build();
    ContainerDeploymentInfoWithNames containerDeploymentInfoWithNames =
        ContainerDeploymentInfoWithNames.builder().clusterName(clusterName).containerSvcName(serviceName).build();
    DeploymentSummary deploymentSummary = DeploymentSummary.builder()
                                              .accountId(accountId)
                                              .containerDeploymentKey(containerDeploymentKey)
                                              .deploymentInfo(containerDeploymentInfoWithNames)
                                              .build();
    Optional<HarnessServiceInfo> harnessServiceInfo =
        cloudToHarnessMappingService.getHarnessServiceInfo(deploymentSummary);
    return harnessServiceInfo.orElse(null);
  }

  private Map<String, InstanceData> getInstanceDataMap(Set<String> instanceIds) {
    return getInstanceDataMap(instanceDataDao.fetchInstanceData(instanceIds));
  }

  private Map<String, InstanceData> getInstanceDataMap(
      String accountId, String clusterId, List<InstanceType> instanceTypes, InstanceState instanceState) {
    List<InstanceData> activeInstanceData =
        instanceDataDao.fetchClusterActiveInstanceData(accountId, clusterId, instanceTypes, instanceState);
    return getInstanceDataMap(activeInstanceData);
  }

  private Map<String, InstanceData> getInstanceDataMap(List<InstanceData> instanceDataList) {
    return instanceDataList.stream().collect(
        Collectors.toMap(InstanceData::getInstanceId, Function.identity(), (existing, replacement) -> existing));
  }

  private InstanceType getInstanceType(Task task) {
    if (task.getLaunchType().equals(LaunchType.EC2.toString())) {
      return InstanceType.ECS_TASK_EC2;
    } else if (task.getLaunchType().equals(LaunchType.FARGATE.toString())) {
      return InstanceType.ECS_TASK_FARGATE;
    }
    return null;
  }

  // TODO check for stop time
  private void updateContainerInstance(String accountId, CECluster ceCluster,
      AwsCrossAccountAttributes awsCrossAccountAttributes, List<ContainerInstance> containerInstances) {
    Instant stopTime = Instant.now();
    String clusterId = ceCluster.getHash();
    String clusterArn = ceCluster.getClusterArn();
    String settingId = ceCluster.getParentAccountSettingId();
    Map<String, InstanceData> activeInstanceDataMap = getInstanceDataMap(
        accountId, clusterId, ImmutableList.of(InstanceType.ECS_CONTAINER_INSTANCE), InstanceState.RUNNING);
    Set<String> activeInstanceIds = activeInstanceDataMap.keySet();
    Set<String> activeInstanceArns =
        containerInstances.stream()
            .map(containerInstance -> getIdFromArn(containerInstance.getContainerInstanceArn()))
            .collect(Collectors.toSet());
    SetView<String> inactiveInstanceArns = Sets.difference(activeInstanceIds, activeInstanceArns);

    activeInstanceDataMap.values()
        .stream()
        .filter(instanceData -> inactiveInstanceArns.contains(instanceData.getInstanceId()))
        .forEach(instanceData -> instanceDataDao.updateInstanceStopTime(instanceData, stopTime));

    Set<String> instanceIds = fetchEc2InstanceIds(containerInstances);
    Map<String, Instance> instanceMap = listEc2Instances(awsCrossAccountAttributes, ceCluster.getRegion(), instanceIds);
    containerInstances.stream()
        .filter(containerInstance -> instanceMap.get(containerInstance.getEc2InstanceId()) != null)
        .forEach(containerInstance -> {
          String containerInstanceId = getIdFromArn(containerInstance.getContainerInstanceArn());
          if (null == activeInstanceDataMap.get(containerInstanceId)) {
            String ec2InstanceId = containerInstance.getEc2InstanceId();
            Instance instance = instanceMap.get(ec2InstanceId);

            List<com.amazonaws.services.ecs.model.Resource> registeredResources =
                containerInstance.getRegisteredResources();
            Map<String, com.amazonaws.services.ecs.model.Resource> resourceMap = registeredResources.stream().collect(
                Collectors.toMap(com.amazonaws.services.ecs.model.Resource::getName, identity()));

            double memory = resourceMap.get("MEMORY").getIntegerValue();
            double cpu = resourceMap.get("CPU").getIntegerValue();

            Map<String, Attribute> attributeMap =
                containerInstance.getAttributes().stream().collect(Collectors.toMap(Attribute::getName, identity()));
            Attribute attribute = attributeMap.get(ECS_OS_TYPE);

            Map<String, String> metaData = new HashMap<>();
            metaData.put(InstanceMetaDataConstants.INSTANCE_FAMILY, instance.getInstanceType());
            metaData.put(InstanceMetaDataConstants.INSTANCE_CATEGORY, getInstanceCategory(instance).name());
            metaData.put(InstanceMetaDataConstants.EC2_INSTANCE_ID, ec2InstanceId);
            metaData.put(InstanceMetaDataConstants.OPERATING_SYSTEM, attribute.getValue());
            metaData.put(InstanceMetaDataConstants.CLOUD_PROVIDER, CloudProvider.AWS.name());
            metaData.put(InstanceMetaDataConstants.REGION, ceCluster.getRegion());
            metaData.put(InstanceMetaDataConstants.CLUSTER_TYPE, ClusterType.ECS.name());
            metaData.put(InstanceMetaDataConstants.CLUSTER_ARN, clusterArn);
            metaData.put(InstanceMetaDataConstants.PARENT_RESOURCE_ID, ec2InstanceId);
            metaData.put(InstanceMetaDataConstants.ACTUAL_PARENT_RESOURCE_ID, ec2InstanceId);
            Resource resource = Resource.builder().cpuUnits(cpu).memoryMb(memory).build();

            Resource totalResource = instanceResourceService.getComputeVMResource(
                InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(
                    InstanceMetaDataConstants.INSTANCE_FAMILY, metaData),
                InstanceMetaDataUtils.getValueForKeyFromInstanceMetaData(InstanceMetaDataConstants.REGION, metaData),
                CloudProvider.AWS);
            InstanceData instanceData = InstanceData.builder()
                                            .accountId(accountId)
                                            .instanceId(containerInstanceId)
                                            .clusterName(getIdFromArn(clusterArn))
                                            .clusterId(clusterId)
                                            .settingId(settingId)
                                            .instanceType(InstanceType.ECS_CONTAINER_INSTANCE)
                                            .instanceState(InstanceState.RUNNING)
                                            .usageStartTime(containerInstance.getRegisteredAt().toInstant())
                                            .totalResource(totalResource)
                                            .allocatableResource(resource)
                                            .metaData(metaData)
                                            .build();
            logger.debug("Creating container instance {} ", containerInstanceId);
            instanceDataService.create(instanceData);
          }
        });
  }

  private InstanceCategory getInstanceCategory(Instance instance) {
    if (null != instance.getSpotInstanceRequestId()) {
      return InstanceCategory.SPOT;
    } else if (null != instance.getCapacityReservationId()) {
      return InstanceCategory.RESERVED;
    }
    return InstanceCategory.ON_DEMAND;
  }

  private String getIdFromArn(String arn) {
    return arn.substring(arn.lastIndexOf('/') + 1);
  }

  private List<Task> listTask(AwsCrossAccountAttributes awsCrossAccountAttributes, String clusterName, String region) {
    List<DesiredStatus> desiredStatuses = listTaskDesiredStatus();
    List<Task> tasks = new ArrayList<>();
    for (DesiredStatus desiredStatus : desiredStatuses) {
      tasks.addAll(
          awsECSHelperService.listTasksForService(awsCrossAccountAttributes, region, clusterName, null, desiredStatus));
    }
    return tasks;
  }

  private void loadTaskArnServiceNameMap(AwsCrossAccountAttributes awsCrossAccountAttributes, String clusterName,
      String region, Map<String, String> taskArnServiceNameMap) {
    List<DesiredStatus> desiredStatuses = listTaskDesiredStatus();
    for (Service service : listServices(awsCrossAccountAttributes, clusterName, region)) {
      for (DesiredStatus desiredStatus : desiredStatuses) {
        List<String> taskArns = awsECSHelperService.listTasksArnForService(
            awsCrossAccountAttributes, region, clusterName, service.getServiceArn(), desiredStatus);
        if (!CollectionUtils.isEmpty(taskArns)) {
          for (String taskArn : taskArns) {
            taskArnServiceNameMap.put(taskArn, service.getServiceArn());
          }
        }
      }
    }
  }

  private List<DesiredStatus> listTaskDesiredStatus() {
    return new ArrayList<>(Arrays.asList(DesiredStatus.RUNNING, DesiredStatus.STOPPED));
  }

  private List<Service> listServices(
      AwsCrossAccountAttributes awsCrossAccountAttributes, String clusterName, String region) {
    return awsECSHelperService.listServicesForCluster(awsCrossAccountAttributes, region, clusterName);
  }

  private Map<String, Instance> listEc2Instances(
      AwsCrossAccountAttributes awsCrossAccountAttributes, String region, Set<String> instanceIds) {
    Map<String, Instance> instanceMap = new HashMap<>();
    if (!CollectionUtils.isEmpty(instanceIds)) {
      List<Instance> instances = awsEC2HelperService.listEc2Instances(awsCrossAccountAttributes, instanceIds, region);
      instanceMap = instances.stream()
                        .filter(instance -> null != instance.getLaunchTime())
                        .collect(Collectors.toMap(Instance::getInstanceId, instance -> instance));
      logger.debug("Instances {} ", instances.toString());
    }
    return instanceMap;
  }

  private Set<String> fetchEc2InstanceIds(List<ContainerInstance> containerInstances) {
    return containerInstances.stream().map(ContainerInstance::getEc2InstanceId).collect(Collectors.toSet());
  }

  private List<ContainerInstance> listContainerInstances(
      AwsCrossAccountAttributes awsCrossAccountAttributes, CECluster ceCluster) {
    List<ContainerInstanceStatus> containerInstanceStatuses = listContainerInstanceStatus();
    List<ContainerInstance> containerInstances = new ArrayList<>();
    for (ContainerInstanceStatus containerInstanceStatus : containerInstanceStatuses) {
      containerInstances.addAll(awsECSHelperService.listContainerInstancesForCluster(
          awsCrossAccountAttributes, ceCluster.getRegion(), ceCluster.getClusterArn(), containerInstanceStatus));
    }
    return containerInstances;
  }

  private List<ContainerInstanceStatus> listContainerInstanceStatus() {
    return new ArrayList<>(Arrays.asList(ContainerInstanceStatus.ACTIVE, ContainerInstanceStatus.DRAINING));
  }

  private Map<String, AwsCrossAccountAttributes> getCrossAccountAttributes(String accountId) {
    List<CECloudAccount> ceCloudAccountList = ceCloudAccountDao.getByAWSAccountId(accountId);
    Map<String, AwsCrossAccountAttributes> crossAccountAttributesMap = ceCloudAccountList.stream().collect(
        Collectors.toMap(CECloudAccount::getInfraAccountId, CECloudAccount::getAwsCrossAccountAttributes));
    List<SettingAttribute> ceConnectorList =
        cloudToHarnessMappingService.listSettingAttributesCreatedInDuration(accountId, CE_CONNECTOR, CE_AWS);
    ceConnectorList.forEach(ceConnector -> {
      CEAwsConfig ceAwsConfig = (CEAwsConfig) ceConnector.getValue();
      crossAccountAttributesMap.put(ceAwsConfig.getAwsMasterAccountId(), ceAwsConfig.getAwsCrossAccountAttributes());
    });
    return crossAccountAttributesMap;
  }
}
