package io.harness.cdng.pipeline.helpers;

import io.harness.beans.ExecutionStrategyType;
import io.harness.cdng.pipeline.NGStepType;
import io.harness.cdng.pipeline.StepCategory;
import io.harness.cdng.pipeline.StepData;
import io.harness.cdng.service.beans.ServiceDefinitionType;
import io.harness.exception.GeneralException;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.io.Resources;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class CDNGPipelineConfigurationHelper {
  @VisibleForTesting static String LIBRARY = "Library";

  private final LoadingCache<ServiceDefinitionType, StepCategory> stepsCache =
      CacheBuilder.newBuilder().build(new CacheLoader<ServiceDefinitionType, StepCategory>() {
        @Override
        public StepCategory load(final ServiceDefinitionType serviceDefinitionType) throws Exception {
          return calculateStepsForServiceDefinitionType(serviceDefinitionType);
        }
      });

  public Map<ServiceDefinitionType, List<ExecutionStrategyType>> getExecutionStrategyList() {
    return Arrays.stream(ServiceDefinitionType.values())
        .collect(Collectors.toMap(
            serviceDefinitionType -> serviceDefinitionType, ServiceDefinitionType::getExecutionStrategies));
  }

  public String getExecutionStrategyYaml(
      ServiceDefinitionType serviceDefinitionType, ExecutionStrategyType executionStrategyType) throws IOException {
    if (ServiceDefinitionType.getExecutionStrategies(serviceDefinitionType).contains(executionStrategyType)) {
      ClassLoader classLoader = this.getClass().getClassLoader();
      return Resources.toString(
          Objects.requireNonNull(classLoader.getResource(
              String.format("executionStrategyYaml/%s-%s.yaml", serviceDefinitionType.getYamlName().toLowerCase(),
                  executionStrategyType.getDisplayName().toLowerCase()))),
          StandardCharsets.UTF_8);
    } else {
      throw new GeneralException("Execution Strategy Not supported for given deployment type");
    }
  }

  public List<ServiceDefinitionType> getServiceDefinitionTypes() {
    return Arrays.asList(ServiceDefinitionType.values());
  }

  public StepCategory getSteps(ServiceDefinitionType serviceDefinitionType) {
    try {
      return stepsCache.get(serviceDefinitionType);
    } catch (ExecutionException e) {
      throw new GeneralException("Exception occurred while calculating the list of steps: " + e.getMessage());
    }
  }

  private StepCategory calculateStepsForServiceDefinitionType(ServiceDefinitionType serviceDefinitionType) {
    List<NGStepType> filteredNGStepTypes =
        Arrays.stream(NGStepType.values())
            .filter(ngStepType -> NGStepType.getServiceDefinitionTypes(ngStepType).contains(serviceDefinitionType))
            .collect(Collectors.toList());
    StepCategory stepCategory = StepCategory.builder().name(LIBRARY).build();
    for (NGStepType stepType : filteredNGStepTypes) {
      addToTopLevel(stepCategory, stepType);
    }
    return stepCategory;
  }

  private void addToTopLevel(StepCategory stepCategory, NGStepType stepType) {
    String categories = NGStepType.getCategory(stepType);
    String[] categoryArrayName = categories.split("/");
    StepCategory currentStepCategory = stepCategory;
    for (String catogoryName : categoryArrayName) {
      currentStepCategory = currentStepCategory.getOrCreateChildStepCategory(catogoryName);
    }
    currentStepCategory.addStepData(
        StepData.builder().name(NGStepType.getDisplayName(stepType)).type(stepType).build());
  }
}
