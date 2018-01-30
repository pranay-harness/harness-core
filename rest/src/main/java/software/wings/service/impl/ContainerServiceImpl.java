package software.wings.service.impl;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.infrastructure.instance.info.EcsContainerInfo.Builder.anEcsContainerInfo;
import static software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder.aKubernetesContainerInfo;
import static software.wings.service.impl.KubernetesHelperService.toYaml;
import static software.wings.utils.EcsConvention.getRevisionFromServiceName;
import static software.wings.utils.EcsConvention.getServiceNamePrefixFromServiceName;
import static software.wings.utils.KubernetesConvention.getPrefixFromControllerName;
import static software.wings.utils.KubernetesConvention.getRevisionFromControllerName;

import com.google.inject.Inject;

import com.amazonaws.services.ecs.model.DescribeTasksRequest;
import com.amazonaws.services.ecs.model.DescribeTasksResult;
import com.amazonaws.services.ecs.model.ListTasksRequest;
import com.amazonaws.services.ecs.model.ListTasksResult;
import com.amazonaws.services.ecs.model.Service;
import com.amazonaws.services.ecs.model.Task;
import io.fabric8.kubernetes.api.model.HasMetadata;
import io.fabric8.kubernetes.api.model.Pod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.AwsConfig;
import software.wings.beans.ErrorCode;
import software.wings.beans.GcpConfig;
import software.wings.beans.KubernetesConfig;
import software.wings.beans.ResponseMessage;
import software.wings.beans.infrastructure.instance.info.ContainerInfo;
import software.wings.cloudprovider.aws.AwsClusterService;
import software.wings.cloudprovider.gke.GkeClusterService;
import software.wings.cloudprovider.gke.KubernetesContainerService;
import software.wings.exception.WingsException;
import software.wings.service.intfc.ContainerService;
import software.wings.settings.SettingValue;
import software.wings.utils.Misc;
import software.wings.utils.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContainerServiceImpl implements ContainerService {
  private static final Logger logger = LoggerFactory.getLogger(ContainerServiceImpl.class);

  @Inject private GkeClusterService gkeClusterService;
  @Inject private KubernetesContainerService kubernetesContainerService;
  @Inject private AwsClusterService awsClusterService;
  @Inject private AwsHelperService awsHelperService;

  @Override
  public Optional<Integer> getServiceDesiredCount(ContainerServiceParams containerServiceParams) {
    if (isNotEmpty(containerServiceParams.getContainerServiceName())) {
      SettingValue value = containerServiceParams.getSettingAttribute().getValue();
      if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
        KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
        return kubernetesContainerService.getControllerPodCount(kubernetesConfig,
            containerServiceParams.getEncryptionDetails(), containerServiceParams.getContainerServiceName());
      } else if (value instanceof AwsConfig) {
        Optional<Service> service =
            awsClusterService
                .getServices(containerServiceParams.getRegion(), containerServiceParams.getSettingAttribute(),
                    containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName())
                .stream()
                .filter(svc -> svc.getServiceName().equals(containerServiceParams.getContainerServiceName()))
                .findFirst();
        if (service.isPresent()) {
          return Optional.of(service.get().getDesiredCount());
        }
      }
    }
    return Optional.empty();
  }

  @Override
  public LinkedHashMap<String, Integer> getActiveServiceCounts(ContainerServiceParams containerServiceParams) {
    LinkedHashMap<String, Integer> result = new LinkedHashMap<>();
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      String controllerNamePrefix = getPrefixFromControllerName(containerServiceParams.getContainerServiceName());
      kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails())
          .stream()
          .filter(ctrl -> ctrl.getMetadata().getName().startsWith(controllerNamePrefix))
          .filter(ctrl -> kubernetesContainerService.getControllerPodCount(ctrl) > 0)
          .filter(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).isPresent())
          .sorted(comparingInt(ctrl -> getRevisionFromControllerName(ctrl.getMetadata().getName()).orElse(-1)))
          .forEach(
              ctrl -> result.put(ctrl.getMetadata().getName(), kubernetesContainerService.getControllerPodCount(ctrl)));
    } else if (value instanceof AwsConfig) {
      String serviceNamePrefix = getServiceNamePrefixFromServiceName(containerServiceParams.getContainerServiceName());
      List<Service> activeOldServices =
          awsClusterService
              .getServices(containerServiceParams.getRegion(), containerServiceParams.getSettingAttribute(),
                  containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName())
              .stream()
              .filter(
                  service -> service.getServiceName().startsWith(serviceNamePrefix) && service.getDesiredCount() > 0)
              .sorted(comparingInt(service -> getRevisionFromServiceName(service.getServiceName())))
              .collect(toList());
      activeOldServices.forEach(service -> result.put(service.getServiceName(), service.getDesiredCount()));
    }
    return result;
  }

  @Override
  public String getDaemonSetYaml(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (!(value instanceof GcpConfig) && !(value instanceof KubernetesConfig)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "DaemonSets apply to kubernetes only");
    }
    KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
    String containerServiceName = containerServiceParams.getContainerServiceName();
    HasMetadata daemonSet = kubernetesContainerService.getController(
        kubernetesConfig, containerServiceParams.getEncryptionDetails(), containerServiceName);
    if (daemonSet != null) {
      try {
        return toYaml(daemonSet);
      } catch (IOException e) {
        logger.error("Error converting DaemonSet to yaml: {}", containerServiceName);
      }
    }
    return null;
  }

  @Override
  public List<String> getActiveAutoscalers(ContainerServiceParams containerServiceParams) {
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (!(value instanceof GcpConfig) && !(value instanceof KubernetesConfig)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT)
          .addParam("args", "Horizontal Pod Autoscalers apply to kubernetes only");
    }
    KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
    String autoscalerNamePrefix = containerServiceParams.getContainerServiceName();

    return kubernetesContainerService.listAutoscalers(kubernetesConfig, containerServiceParams.getEncryptionDetails())
        .stream()
        .filter(autoscaler -> autoscaler.getMetadata().getName().startsWith(autoscalerNamePrefix))
        .filter(autoscaler -> !"none".equals(autoscaler.getSpec().getScaleTargetRef().getName()))
        .map(autoscaler -> autoscaler.getMetadata().getName())
        .collect(toList());
  }

  private KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParams) {
    KubernetesConfig kubernetesConfig;
    if (containerServiceParams.getSettingAttribute().getValue() instanceof GcpConfig) {
      kubernetesConfig = gkeClusterService.getCluster(containerServiceParams.getSettingAttribute(),
          containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName(),
          containerServiceParams.getNamespace());
      kubernetesConfig.setDecrypted(true);
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
    if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      Validator.notNullCheck("KubernetesConfig", kubernetesConfig);
      HasMetadata controller = kubernetesContainerService.getController(
          kubernetesConfig, containerServiceParams.getEncryptionDetails(), containerServiceName);

      if (controller != null) {
        Map<String, String> labels = controller.getMetadata().getLabels();
        List<io.fabric8.kubernetes.api.model.Service> services =
            kubernetesContainerService
                .getServices(kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels)
                .getItems();
        String serviceName = services.isEmpty() ? "None" : services.get(0).getMetadata().getName();
        for (Pod pod :
            kubernetesContainerService.getPods(kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels)
                .getItems()) {
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
                                                .withMaxResults(100)
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
    try {
      if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
        KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
        kubernetesContainerService.listControllers(kubernetesConfig, containerServiceParams.getEncryptionDetails());
        return true;
      } else if (value instanceof AwsConfig) {
        awsClusterService.getServices(containerServiceParams.getRegion(), containerServiceParams.getSettingAttribute(),
            containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName());
        return true;
      }
    } catch (Exception ex) {
      logger.error(ex.getMessage(), ex);
      if (ex instanceof WingsException) {
        throw ex;
      }
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, ex).addParam("args", Misc.getMessage(ex));
    }
    return false;
  }
}
