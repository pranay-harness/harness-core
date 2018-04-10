package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static software.wings.beans.infrastructure.instance.info.EcsContainerInfo.Builder.anEcsContainerInfo;
import static software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder.aKubernetesContainerInfo;

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
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.aws.EcsContainerService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.exception.WingsException.ReportTarget;
import software.wings.helpers.ext.azure.AzureHelperService;
import software.wings.service.intfc.ContainerService;
import software.wings.settings.SettingValue;
import software.wings.utils.Validator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
  public LinkedHashMap<String, Integer> getActiveServiceCounts(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (isKubernetesClusterConfig(value)) {
      return kubernetesContainerService.getActiveServiceCounts(getKubernetesConfig(containerServiceParams),
          containerServiceParams.getEncryptionDetails(), containerServiceParams.getContainerServiceName());
    } else if (value instanceof AwsConfig) {
      return awsClusterService.getActiveServiceCounts(containerServiceParams.getRegion(),
          containerServiceParams.getSettingAttribute(), containerServiceParams.getEncryptionDetails(),
          containerServiceParams.getClusterName(), containerServiceParams.getContainerServiceName());
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
    List<ContainerInfo> result = new ArrayList<>();
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    String containerServiceName = containerServiceParams.getContainerServiceName();
    if (isKubernetesClusterConfig(value)) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      Validator.notNullCheck("KubernetesConfig", kubernetesConfig);
      HasMetadata controller = kubernetesContainerService.getController(
          kubernetesConfig, containerServiceParams.getEncryptionDetails(), containerServiceName);

      if (controller != null) {
        Map<String, String> labels = controller.getMetadata().getLabels();
        List<io.fabric8.kubernetes.api.model.Service> services = kubernetesContainerService.getServices(
            kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels);
        String serviceName = services.isEmpty() ? "None" : services.get(0).getMetadata().getName();
        for (Pod pod : kubernetesContainerService.getPods(
                 kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels)) {
          if (pod.getStatus().getPhase().equals("Running")) {
            result.add(aKubernetesContainerInfo()
                           .withClusterName(containerServiceParams.getClusterName())
                           .withPodName(pod.getMetadata().getName())
                           .withControllerName(containerServiceName)
                           .withServiceName(serviceName)
                           .build());
          }
        }
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
            if (ErrorCode.AWS_CLUSTER_NOT_FOUND.getCode().equals(errorCode.getCode())) {
              logger.info("ECS Cluster not found for service name:" + containerServiceName);
              continue;
            } else if (ErrorCode.AWS_SERVICE_NOT_FOUND.getCode().equals(errorCode.getCode())) {
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
        kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
        return true;
      } else {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, "Invalid Argument: Not a valid AKS cluster");
      }
    } else if (value instanceof KubernetesClusterConfig) {
      KubernetesClusterConfig kubernetesClusterConfig = (KubernetesClusterConfig) value;
      KubernetesConfig kubernetesConfig =
          kubernetesClusterConfig.createKubernetesConfig(containerServiceParams.getNamespace());
      kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      return true;
    } else if (isKubernetesClusterConfig(value)) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
      return true;
    }

    throw new WingsException(ErrorCode.INVALID_ARGUMENT, ReportTarget.USER)
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
          containerServiceParams.getRegion(), containerServiceParams.getEncryptionDetails(), null, awsConfig, taskArns);
    }

    throw new WingsException("invalid setting type " + containerServiceParams);
  }
}
