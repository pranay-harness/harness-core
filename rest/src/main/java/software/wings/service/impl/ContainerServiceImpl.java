package software.wings.service.impl;

import static java.util.Comparator.comparingInt;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.wings.beans.infrastructure.instance.info.EcsContainerInfo.Builder.anEcsContainerInfo;
import static software.wings.beans.infrastructure.instance.info.KubernetesContainerInfo.Builder.aKubernetesContainerInfo;
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
import io.fabric8.kubernetes.api.KubernetesHelper;
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
import software.wings.utils.Validator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ContainerServiceImpl implements ContainerService {
  private final Logger logger = LoggerFactory.getLogger(getClass());

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
    String previousDaemonSetYaml = null;
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      String containerServiceName = containerServiceParams.getContainerServiceName();
      HasMetadata daemonSet = kubernetesContainerService.getController(
          kubernetesConfig, containerServiceParams.getEncryptionDetails(), containerServiceName);
      if (daemonSet != null) {
        try {
          previousDaemonSetYaml = KubernetesHelper.toYaml(daemonSet);
        } catch (IOException e) {
          logger.error("Error converting DaemonSet to yaml: {}", containerServiceName);
        }
      }
    }
    return previousDaemonSetYaml;
  }

  private KubernetesConfig getKubernetesConfig(ContainerServiceParams containerServiceParams) {
    return containerServiceParams.getSettingAttribute().getValue() instanceof GcpConfig
        ? gkeClusterService.getCluster(containerServiceParams.getSettingAttribute(),
              containerServiceParams.getEncryptionDetails(), containerServiceParams.getClusterName(),
              containerServiceParams.getNamespace())
        : (KubernetesConfig) containerServiceParams.getSettingAttribute().getValue();
  }

  @Override
  public List<ContainerInfo> getContainerInfos(ContainerServiceParams containerServiceParams) {
    List<ContainerInfo> result = new ArrayList<>();
    SettingValue value = containerServiceParams.getSettingAttribute().getValue();
    if (value instanceof GcpConfig || value instanceof KubernetesConfig) {
      KubernetesConfig kubernetesConfig = getKubernetesConfig(containerServiceParams);
      Validator.notNullCheck("KubernetesConfig", kubernetesConfig);

      HasMetadata controller = kubernetesContainerService.getController(kubernetesConfig,
          containerServiceParams.getEncryptionDetails(), containerServiceParams.getContainerServiceName());

      if (controller != null) {
        Map<String, String> labels = controller.getMetadata().getLabels();
        List<io.fabric8.kubernetes.api.model.Service> services =
            kubernetesContainerService
                .getServices(kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels)
                .getItems();
        String serviceName = services.size() > 0 ? services.get(0).getMetadata().getName() : "None";
        for (Pod pod :
            kubernetesContainerService.getPods(kubernetesConfig, containerServiceParams.getEncryptionDetails(), labels)
                .getItems()) {
          if (pod.getStatus().getPhase().equals("Running")) {
            List<? extends HasMetadata> controllers = kubernetesContainerService.getControllers(
                kubernetesConfig, containerServiceParams.getEncryptionDetails(), pod.getMetadata().getLabels());
            String controllerName = controllers.size() > 0 ? controllers.get(0).getMetadata().getName() : "None";
            result.add(aKubernetesContainerInfo()
                           .withClusterName(containerServiceParams.getClusterName())
                           .withPodName(pod.getMetadata().getName())
                           .withControllerName(controllerName)
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
                                                .withServiceName(containerServiceParams.getContainerServiceName())
                                                .withMaxResults(100)
                                                .withNextToken(nextToken)
                                                .withDesiredStatus("RUNNING");
        ListTasksResult listTasksResult;
        try {
          listTasksResult = awsHelperService.listTasks(containerServiceParams.getRegion(), awsConfig,
              containerServiceParams.getEncryptionDetails(), listTasksRequest);
        } catch (WingsException ex) {
          // if the cluster / service has been deleted, we need to continue and check the rest of the service names
          List<ResponseMessage> responseMessageList = ex.getResponseMessageList();
          if (!responseMessageList.isEmpty()) {
            ErrorCode errorCode = responseMessageList.get(0).getCode();
            if (errorCode != null) {
              if (ErrorCode.AWS_CLUSTER_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                logger.info(
                    "ECS Cluster not found for service name:" + containerServiceParams.getContainerServiceName());
                continue;
              } else if (ErrorCode.AWS_SERVICE_NOT_FOUND.getCode().equals(errorCode.getCode())) {
                logger.info(
                    "ECS Service not found for service name:" + containerServiceParams.getContainerServiceName());
                continue;
              }
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
                             .withServiceName(containerServiceParams.getContainerServiceName())
                             .build());
            }
          }
        }
        nextToken = listTasksResult.getNextToken();
      } while (nextToken != null);
    }
    return result;
  }
}
