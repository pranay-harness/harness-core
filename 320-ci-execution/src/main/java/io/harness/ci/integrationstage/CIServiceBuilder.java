package io.harness.ci.integrationstage;

import static io.harness.common.CIExecutionConstants.IMAGE_PATH_SPLIT_REGEX;
import static io.harness.common.CIExecutionConstants.SERVICE_PREFIX;
import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.dependencies.CIServiceInfo;
import io.harness.beans.dependencies.DependencyElement;
import io.harness.beans.environment.pod.container.ContainerDefinitionInfo;
import io.harness.beans.environment.pod.container.ContainerImageDetails;
import io.harness.beans.serializer.RunTimeInputHandler;
import io.harness.beans.stages.IntegrationStageConfig;
import io.harness.beans.yaml.extended.container.ContainerResource;
import io.harness.beans.yaml.extended.container.quantity.unit.BinaryQuantityUnit;
import io.harness.beans.yaml.extended.container.quantity.unit.DecimalQuantityUnit;
import io.harness.ci.config.CIExecutionServiceConfig;
import io.harness.ci.utils.QuantityUtils;
import io.harness.delegate.beans.ci.pod.CIContainerType;
import io.harness.delegate.beans.ci.pod.ContainerResourceParams;
import io.harness.exception.InvalidRequestException;
import io.harness.k8s.model.ImageDetails;
import io.harness.plancreator.stages.stage.StageElementConfig;
import io.harness.stateutils.buildstate.providers.ServiceContainerUtils;
import io.harness.util.PortFinder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CIServiceBuilder {
  public static List<ContainerDefinitionInfo> createServicesContainerDefinition(
      StageElementConfig stageElementConfig, PortFinder portFinder, CIExecutionServiceConfig ciExecutionServiceConfig) {
    List<ContainerDefinitionInfo> containerDefinitionInfos = new ArrayList<>();
    IntegrationStageConfig integrationStage = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);
    if (isEmpty(integrationStage.getServiceDependencies())) {
      return containerDefinitionInfos;
    }

    int serviceIdx = 0;
    for (DependencyElement dependencyElement : integrationStage.getServiceDependencies()) {
      if (dependencyElement == null) {
        continue;
      }

      if (dependencyElement.getDependencySpecType() instanceof CIServiceInfo) {
        ContainerDefinitionInfo containerDefinitionInfo =
            createServiceContainerDefinition((CIServiceInfo) dependencyElement.getDependencySpecType(), portFinder,
                serviceIdx, ciExecutionServiceConfig);
        if (containerDefinitionInfo != null) {
          containerDefinitionInfos.add(containerDefinitionInfo);
        }
        serviceIdx++;
      }
    }
    return containerDefinitionInfos;
  }

  private static ContainerDefinitionInfo createServiceContainerDefinition(
      CIServiceInfo service, PortFinder portFinder, int serviceIdx, CIExecutionServiceConfig ciExecutionServiceConfig) {
    Integer port = portFinder.getNextPort();
    service.setGrpcPort(port);

    String containerName = String.format("%s%d", SERVICE_PREFIX, serviceIdx);
    return ContainerDefinitionInfo.builder()
        .name(containerName)
        .commands(ServiceContainerUtils.getCommand())
        .args(ServiceContainerUtils.getArguments(
            service.getIdentifier(), service.getImage(), service.getEntrypoint(), service.getArgs(), port))
        .envVars(service.getEnvironment())
        .containerImageDetails(ContainerImageDetails.builder()
                                   .imageDetails(getImageInfo(service.getImage()))
                                   .connectorIdentifier(service.getConnector())
                                   .build())
        .containerResourceParams(
            getServiceContainerResource(service.getResources(), ciExecutionServiceConfig, service.getIdentifier()))
        .ports(Collections.singletonList(port))
        .containerType(CIContainerType.SERVICE)
        .stepIdentifier(service.getIdentifier())
        .stepName(service.getName())
        .build();
  }

  private static ContainerResourceParams getServiceContainerResource(
      ContainerResource resource, CIExecutionServiceConfig ciExecutionServiceConfig, String identifier) {
    Integer cpu = ciExecutionServiceConfig.getDefaultCPULimit();
    Integer memory = ciExecutionServiceConfig.getDefaultMemoryLimit();

    if (resource != null && resource.getLimits() != null) {
      if (resource.getLimits().getCpu() != null) {
        String cpuQuantity = RunTimeInputHandler.resolveStringParameter(
            "cpu", "Service", identifier, resource.getLimits().getCpu(), false);
        cpu = QuantityUtils.getCpuQuantityValueInUnit(cpuQuantity, DecimalQuantityUnit.m);
      }
      if (resource.getLimits().getMemory() != null) {
        String memoryQuantity = RunTimeInputHandler.resolveStringParameter(
            "memory", "Service", identifier, resource.getLimits().getMemory(), false);
        memory = QuantityUtils.getMemoryQuantityValueInUnit(memoryQuantity, BinaryQuantityUnit.Mi);
      }
    }
    return ContainerResourceParams.builder()
        .resourceRequestMilliCpu(cpu)
        .resourceRequestMemoryMiB(memory)
        .resourceLimitMilliCpu(cpu)
        .resourceLimitMemoryMiB(memory)
        .build();
  }

  public static List<String> getServiceIdList(StageElementConfig stageElementConfig) {
    List<String> serviceIdList = new ArrayList<>();
    IntegrationStageConfig integrationStage = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    if (isEmpty(integrationStage.getServiceDependencies())) {
      return serviceIdList;
    }

    for (DependencyElement dependencyElement : integrationStage.getServiceDependencies()) {
      if (dependencyElement == null) {
        continue;
      }

      if (dependencyElement.getDependencySpecType() instanceof CIServiceInfo) {
        serviceIdList.add(((CIServiceInfo) dependencyElement.getDependencySpecType()).getIdentifier());
      }
    }
    return serviceIdList;
  }

  public static List<Integer> getServiceGrpcPortList(StageElementConfig stageElementConfig) {
    List<Integer> grpcPortList = new ArrayList<>();
    IntegrationStageConfig integrationStage = IntegrationStageUtils.getIntegrationStageConfig(stageElementConfig);

    if (isEmpty(integrationStage.getServiceDependencies())) {
      return grpcPortList;
    }

    for (DependencyElement dependencyElement : integrationStage.getServiceDependencies()) {
      if (dependencyElement == null) {
        continue;
      }

      if (dependencyElement.getDependencySpecType() instanceof CIServiceInfo) {
        grpcPortList.add(((CIServiceInfo) dependencyElement.getDependencySpecType()).getGrpcPort());
      }
    }
    return grpcPortList;
  }

  private static ImageDetails getImageInfo(String image) {
    String tag = "";
    String name = image;

    if (image.contains(IMAGE_PATH_SPLIT_REGEX)) {
      String[] subTokens = image.split(IMAGE_PATH_SPLIT_REGEX);
      if (subTokens.length > 2) {
        throw new InvalidRequestException("Image should not contain multiple tags");
      }
      if (subTokens.length == 2) {
        name = subTokens[0];
        tag = subTokens[1];
      }
    }

    return ImageDetails.builder().name(name).tag(tag).build();
  }
}
