package io.harness.cdng.stepsdependency.utils;

import com.google.common.annotations.VisibleForTesting;

import io.harness.ambiance.Ambiance;
import io.harness.cdng.executionplan.CDStepDependencyKey;
import io.harness.cdng.infra.yaml.Infrastructure;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.CreateExecutionPlanContext;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.stepsdependency.resolvers.StepDependencyResolverContextImpl;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import lombok.experimental.UtilityClass;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public class CDStepDependencyUtils {
  @VisibleForTesting
  Map<String, List<ResolvedRefInput>> getRefKeyToInputParamsMap(StepInputPackage stepInputPackage) {
    Map<String, List<ResolvedRefInput>> resultMap = new HashMap<>();
    for (ResolvedRefInput input : stepInputPackage.getInputs()) {
      if (!resultMap.containsKey(input.getRefObject().getKey())) {
        resultMap.put(input.getRefObject().getKey(), new LinkedList<>());
      }
      resultMap.get(input.getRefObject().getKey()).add(input);
    }

    return resultMap;
  }

  public String getServiceKey(CreateExecutionPlanContext context) {
    return ParentPathInfoUtils.getParentPath(PlanNodeType.STAGE.name(), context) + "."
        + CDStepDependencyKey.SERVICE.name();
  }

  public String getInfraKey(CreateExecutionPlanContext context) {
    return ParentPathInfoUtils.getParentPath(PlanNodeType.STAGE.name(), context) + "."
        + CDStepDependencyKey.INFRASTRUCTURE.name();
  }

  public ServiceOutcome getService(StepDependencyService dependencyService, StepDependencySpec serviceSpec,
      StepInputPackage stepInputPackage, StepParameters stepParameters, Ambiance ambiance) {
    StepDependencyResolverContext resolverContext =
        getStepDependencyResolverContext(stepInputPackage, stepParameters, ambiance);
    Optional<ServiceOutcome> resolve = dependencyService.resolve(serviceSpec, resolverContext);
    return resolve.orElseThrow(() -> new InvalidArgumentsException("Service Dependency is not available."));
  }

  public Infrastructure getInfrastructure(StepDependencyService dependencyService, StepDependencySpec infraSpec,
      StepInputPackage stepInputPackage, StepParameters stepParameters, Ambiance ambiance) {
    StepDependencyResolverContext resolverContext =
        getStepDependencyResolverContext(stepInputPackage, stepParameters, ambiance);
    Optional<Infrastructure> resolve = dependencyService.resolve(infraSpec, resolverContext);
    return resolve.orElseThrow(() -> new InvalidArgumentsException("Infrastructure Dependency is not available."));
  }

  public K8sRollingOutcome getK8sRolling(StepDependencyService dependencyService, StepDependencySpec k8sRollingSpec,
      StepInputPackage stepInputPackage, StepParameters stepParameters, Ambiance ambiance) {
    StepDependencyResolverContext resolverContext =
        getStepDependencyResolverContext(stepInputPackage, stepParameters, ambiance);
    Optional<K8sRollingOutcome> resolve = dependencyService.resolve(k8sRollingSpec, resolverContext);
    return resolve.orElseThrow(() -> new InvalidArgumentsException("K8s Rolling Dependency is not available."));
  }

  public StepDependencyResolverContext getStepDependencyResolverContext(
      StepInputPackage stepInputPackage, StepParameters stepParameters, Ambiance ambiance) {
    Map<String, List<ResolvedRefInput>> refToInputParamsMap = getRefKeyToInputParamsMap(stepInputPackage);
    return StepDependencyResolverContextImpl.builder()
        .stepInputPackage(stepInputPackage)
        .ambiance(ambiance)
        .refKeyToInputParamsMap(refToInputParamsMap)
        .stepParameters(stepParameters)
        .build();
  }
}
