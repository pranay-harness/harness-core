package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static software.wings.beans.infrastructure.instance.info.EcsContainerInfo.Builder.anEcsContainerInfo;
import static software.wings.common.Constants.HARNESS_REVISION;
import static software.wings.exception.WingsException.USER;

import com.google.common.collect.Sets;
import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Task;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.AzureConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesClusterConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ResponseMessage;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.ContainerService;
import software.wings.settings.SettingValue;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ContainerServiceImpl implements ContainerService {
  private static final Logger logger = LoggerFactory.getLogger(ContainerServiceImpl.class);

  @Inject private GkeClusterService gkeClusterService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private AwsClusterService awsClusterService;
  @Inject private AwsHelperService awsHelperService;
  @Inject private AzureHelperService azureHelperService;
  @Inject private EcsContainerService ecsContainerService;

  private boolean isKubernetesClusterConfig(SettingValue value) {
    return value instanceof AzureConfig || value instanceof GcpConfig || value instanceof KubernetesConfig
        || value instanceof KubernetesClusterConfig;
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(
      ContainerServiceParams containerServiceParams, boolean useDashInHostname) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    String controllerName = containerServiceParams.getContainerServiceName();
    if (isKubernetesClusterConfig(value)) {
      return kubernetesContainerService.getActiveServiceCounts(getKubernetesConfig(containerServiceParams),
          containerServiceParams.getEncryptionDetails(), controllerName, useDashInHostname);
    } else if (value instanceof AwsConfig) {
      return awsClusterService.getActiveServiceCounts(containerServiceParams.getRegion(),
          containerServiceParams.getSettingAttribute(), containerServiceParams.getEncryptionDetails(),
          containerServiceParams.getClusterName(), controllerName);
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Unknown setting value type for container service: " + value.getType());
    }
  }

  private KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParams) {
    KubernetesConfig kubernetesConfig;
    if (containerServiceParams.getSettingAttribute().getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(containerServiceParams.getSettingAttribute(),
          containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName(),
          containerServiceParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
    } else if (containerServiceParams.getSettingAttribute().getValue() instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) containerServiceParams.getSettingAttribute().getValue();
      kubernetesConfig =
          azureHelperService.getKubernetesClusterConfig(azureConfig, containerServiceParams.getEncryptionDetails(),
              containerServiceParams.getSubscriptionId(), containerServiceParams.getResourceGroup(),
              containerServiceParams.getClusterName(), containerServiceParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
    } else if (containerServiceParams.getSettingAttribute().getValue() instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig =
          (KubernetesClusterConfig) containerServiceParams.getSettingAttribute().getValue();
      kubernetesConfig = kubernetesClusterConfig.createKubernetesConfig(containerServiceParams.getNamespace());
    } else {
      kubernetesConfig = (KubernetesConfig) containerServiceParams.getSettingAttribute().getValue();
    }
    return kubernetesConfig;
  }

  @Override
  public List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams) {
    String containerServiceName = containerServiceParams.getContainerServiceName();
    String accountId = containerServiceParams.getSettingAttribute().getAccountId();
    logger.info("Getting container infos for account {}, controller: {}", accountId, containerServiceName);
    List<ContainerInfo> result = new ArrayList<>();
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();

    if (isKubernetesClusterConfig(value)) {
      logger.info("Kubernetes cluster config for account {}, controller: {}", accountId, containerServiceName);
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      Validator.notNullCheck("KubernetesConfig", kubernetesConfig);
      HasMetadata controller = kubernetesContainerService.getController(
          kubernetesConfig, containerServiceParams.getEncryptionDetails(), containerServiceName);
      if (controller != null) {
        logger.info("Got controller {} for account {}", controller.getMetadata().getName(), accountId);
        Map<String, String> labels =
            kubernetesContainerService.getPodTemplateSpec(controller).getMetadata().getLabels();
        Map<String, String> serviceLabels = new HashMap<>(labels);
        serviceLabels.remove(HARNESS_REVISION);
        List<io.fabric8.kubernetes.api.model.Service> services = kubernetesContainerService.getServices(
            kubernetesConfig, containerServiceParams.getEncryptionDetails(), serviceLabels);
        String serviceName = services.isEmpty() ? "None" : services.get(0).getMetadata().getName();
        logger.info("Got Service {} for controller {} for account {}", serviceName, containerServiceName, accountId);
        List<Pod> pods =
            kubernetesContainerService.getPods(kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);
        logger.info("Got {} pods for controller {} for account {}", pods != null ? pods.size() : 0,
            containerServiceName, accountId);
        if (isEmpty(pods)) {
          return result;
        }

        for (Pod pod : pods) {
          String phase = pod.getStatus().getPhase();
          logger.info("Phase: {} for pod {} for controller {} for account {}", pod.getStatus().getPhase(),
              pod.getMetadata().getName(), containerServiceName, accountId);
          if ("Running".equals(phase)) {
            result.add(KubernetesContainerInfo.builder()
                           .clusterName(containerServiceParams.getClusterName())
                           .podName(pod.getMetadata().getName())
                           .ip(pod.getStatus().getPodIP())
                           .controllerName(containerServiceName)
                           .serviceName(serviceName)
                           .build());
          }
        }
      } else {
        logger.info("Could not get controller {} for account {}", containerServiceName, accountId);
      }
    } else if (value instanceof AwsConfig) {
      AwsConfig awsConfig = awsHelperService.validateAndGetAwsConfig(
          containerServiceParams.getSettingAttribute(), containerServiceParams.getEncryptionDetails());
      Validator.notNullCheck("AwsConfig", awsConfig);

      List<Task> tasks;

      String nextToken = null;
      do {
        ListTasksRequest listTasksRequest = new ListTasksRequest()
                                                .withCluster(containerServiceParams.getClusterName())
                                                .withServiceName(containerServiceName)
                                                //.withMaxResults(100)
                                                .withNextToken(nextToken)
                                                .withDesiredStatus("RUNNING");
        ListTasksResult listTasksResult;
        try {
          listTasksResult = awsHelperService.listTasks(containerServiceParams.getRegion(), awsConfig,
              containerServiceParams.getEncryptionDetails(), listTasksRequest);
        } catch (WingsException ex) {
          // if the cluster / service has been deleted, we need to continue and check the rest of the service names
          ResponseMessage responseMessage = ex.getResponseMessage();
          ErrorCode errorCode = responseMessage.getCode();
          if (errorCode != null) {
            if (ErrorCode.AWS_CLUSTER_NOT_FOUND == errorCode) {
              logger.info("ECS Cluster not found for service name:" + containerServiceName);
              continue;
            } else if (ErrorCode.AWS_SERVICE_NOT_FOUND == errorCode) {
              logger.info("ECS Service not found for service name:" + containerServiceName);
              continue;
            }
          }
          throw ex;
        }

        if (!listTasksResult.getTaskArns().isEmpty()) {
          DescribeTasksRequest describeTasksRequest = new DescribeTasksRequest()
                                                          .withCluster(containerServiceParams.getClusterName())
                                                          .withTasks(listTasksResult.getTaskArns());
          DescribeTasksResult describeTasksResult = awsHelperService.describeTasks(containerServiceParams.getRegion(),
              awsConfig, containerServiceParams.getEncryptionDetails(), describeTasksRequest);
          tasks = describeTasksResult.getTasks();
          for (Task task : tasks) {
            if (task != null) {
              result.add(anEcsContainerInfo()
                             .withClusterName(containerServiceParams.getClusterName())
                             .withTaskDefinitionArn(task.getTaskDefinitionArn())
                             .withTaskArn(task.getTaskArn())
                             .withVersion(task.getVersion())
                             .withStartedAt(task.getStartedAt() == null ? 0L : task.getStartedAt().getTime())
                             .withStartedBy(task.getStartedBy())
                             .withServiceName(containerServiceName)
                             .build());
            }
          }
        }
        nextToken = listTasksResult.getNextToken();
      } while (nextToken != null);
    }
    return result;
  }

  @Override
  public Set<String> getControllerNames(ContainerServiceParams containerServiceParams, Map<String, String> labels) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (isKubernetesClusterConfig(value)) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      Validator.notNullCheck("KubernetesConfig", kubernetesConfig);
      List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(
          kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);

      if (isNotEmpty(controllers)) {
        return controllers.stream().map(controller -> controller.getMetadata().getName()).collect(Collectors.toSet());
      }
    }

    return Sets.newHashSet();
  }

  @Override
  public Boolean validate(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof AwsConfig) {
      awsClusterService.getServices(containerServiceParams.getRegion(), containerServiceParams.getSettingAttribute(),
          containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName());
      return true;
    } else if (value instanceof AzureConfig) {
      AzureConfig azureConfig = (AzureConfig) containerServiceParams.getSettingAttribute().getValue();
      if (azureHelperService.isValidKubernetesCluster(azureConfig, containerServiceParams.getEncryptionDetails(),
              containerServiceParams.getSubscriptionId(), containerServiceParams.getResourceGroup(),
              containerServiceParams.getClusterName())) {
        KubernetesConfig kubernetesConfig =
            azureHelperService.getKubernetesClusterConfig(azureConfig, containerServiceParams.getEncryptionDetails(),
                containerServiceParams.getSubscriptionId(), containerServiceParams.getResourceGroup(),
                containerServiceParams.getClusterName(), containerServiceParams.getNamespace());
        kubernetesConfig.setDecrypted(true);
        // TODO - switch to list namespaces?
        kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
        // kubernetesContainerService.listNamespaces(kubernetesConfig, containerServiceParams.getEncryptionDetails());
        return true;
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "Invalid Argument: Not a valid AKS cluster");
      }
    } else if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      KubernetesConfig kubernetesConfig =
          kubernetesClusterConfig.createKubernetesConfig(containerServiceParams.getNamespace());
      // TODO - switch to list namespaces?
      kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      // kubernetesContainerService.listNamespaces(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      return true;
    } else if (isKubernetesClusterConfig(value)) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      // TODO - switch to list namespaces?
      kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      // kubernetesContainerService.listNamespaces(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      return true;
    }

    throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
        .addParam("args", "Unknown setting value type: " + value.getType());
  }

  @Override
  public List<software.wings.cloudprovider.ContainerInfo> fetchContainerInfos(
      ContainerServiceParams containerServiceParams) {
    if (containerServiceParams.getSettingAttribute().getValue() instanceof AwsConfig) {
      AwsConfig awsConfig = (AwsConfig) containerServiceParams.getSettingAttribute().getValue();
      List<Task> tasks = new ArrayList<>();
      containerServiceParams.getContainerServiceNames().forEach(serviceName -> {
        ListTasksRequest listTasksRequest = new ListTasksRequest()
                                                .withCluster(containerServiceParams.getClusterName())
                                                .withServiceName(serviceName)
                                                .withDesiredStatus("RUNNING");

        ListTasksResult listTasksResult = awsHelperService.listTasks(containerServiceParams.getRegion(), awsConfig,
            containerServiceParams.getEncryptionDetails(), listTasksRequest);

        if (listTasksResult != null && !isEmpty(listTasksResult.getTaskArns())) {
          DescribeTasksRequest describeTasksRequest = new DescribeTasksRequest()
                                                          .withCluster(containerServiceParams.getClusterName())
                                                          .withTasks(listTasksResult.getTaskArns());
          DescribeTasksResult describeTasksResult = awsHelperService.describeTasks(containerServiceParams.getRegion(),
              awsConfig, containerServiceParams.getEncryptionDetails(), describeTasksRequest);
          tasks.addAll(describeTasksResult.getTasks());
        }
      });

      List<String> taskArns = tasks.stream().map(Task::getTaskArn).collect(Collectors.toList());

      return ecsContainerService.generateContainerInfos(tasks, containerServiceParams.getClusterName(),
          containerServiceParams.getRegion(), containerServiceParams.getEncryptionDetails(), null, awsConfig, taskArns,
          taskArns);
    }

    throw new WingsException("invalid setting type " + containerServiceParams);
  }
}
