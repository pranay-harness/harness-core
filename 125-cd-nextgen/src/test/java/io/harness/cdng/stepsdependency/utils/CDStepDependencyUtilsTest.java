package io.harness.cdng.stepsdependency.utils;

import static io.harness.rule.OwnerRule.ARCHIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;

import io.harness.CategoryTest;
import io.harness.ambiance.Ambiance;
import io.harness.category.element.UnitTests;
import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.beans.K8sDirectInfrastructureOutcome;
import io.harness.cdng.k8s.K8sRollingOutcome;
import io.harness.cdng.service.beans.ServiceOutcome;
import io.harness.data.Outcome;
import io.harness.exception.InvalidArgumentsException;
import io.harness.executionplan.core.ExecutionPlanCreationContext;
import io.harness.executionplan.core.impl.ExecutionPlanCreationContextImpl;
import io.harness.executionplan.plancreator.beans.PlanLevelNode;
import io.harness.executionplan.plancreator.beans.PlanNodeType;
import io.harness.executionplan.stepsdependency.StepDependencyResolverContext;
import io.harness.executionplan.stepsdependency.StepDependencyService;
import io.harness.executionplan.stepsdependency.StepDependencySpec;
import io.harness.executionplan.utils.ParentPathInfoUtils;
import io.harness.references.OutcomeRefObject;
import io.harness.rule.Owner;
import io.harness.state.io.ResolvedRefInput;
import io.harness.state.io.StepInputPackage;
import io.harness.state.io.StepParameters;
import io.harness.state.io.StepTransput;
import io.harness.steps.dummy.DummyStepParameters;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.junit.Rule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class CDStepDependencyUtilsTest extends CategoryTest {
  @Rule public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock StepDependencyService dependencyService;

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetRefKeyToInputParamsMap() {
    StepTransput transput = new DummyOutcome("value");
    StepInputPackage inputPackage = StepInputPackage.builder()
                                        .input(ResolvedRefInput.builder()
                                                   .refObject(OutcomeRefObject.builder().key("TEST").build())
                                                   .transput(transput)
                                                   .build())
                                        .input(ResolvedRefInput.builder()
                                                   .refObject(OutcomeRefObject.builder().key("TEST").build())
                                                   .transput(transput)
                                                   .build())
                                        .build();
    Map<String, List<ResolvedRefInput>> resultMap = CDStepDependencyUtils.getRefKeyToInputParamsMap(inputPackage);
    assertThat(resultMap.keySet().size()).isEqualTo(1);
    assertThat(resultMap.get("TEST").size()).isEqualTo(2);
    assertThat(resultMap.get("TEST").get(0).getRefObject().getKey()).isEqualTo("TEST");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetServiceKey() {
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    PlanLevelNode planLevelNode = PlanLevelNode.builder()
                                      .planNodeType(PlanNodeType.PIPELINE.name())
                                      .identifier(PlanNodeType.PIPELINE.name())
                                      .build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    planLevelNode =
        PlanLevelNode.builder().planNodeType(PlanNodeType.STAGE.name()).identifier(PlanNodeType.STAGE.name()).build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    String serviceKey = CDStepDependencyUtils.getServiceKey(context);
    assertThat(serviceKey).isEqualTo("PIPELINE.STAGE.SERVICE");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetInfraKey() {
    ExecutionPlanCreationContext context = ExecutionPlanCreationContextImpl.builder().build();
    PlanLevelNode planLevelNode = PlanLevelNode.builder()
                                      .planNodeType(PlanNodeType.PIPELINE.name())
                                      .identifier(PlanNodeType.PIPELINE.name())
                                      .build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    planLevelNode =
        PlanLevelNode.builder().planNodeType(PlanNodeType.STAGE.name()).identifier(PlanNodeType.STAGE.name()).build();
    ParentPathInfoUtils.addToParentPath(context, planLevelNode);
    String infraKey = CDStepDependencyUtils.getInfraKey(context);
    assertThat(infraKey).isEqualTo("PIPELINE.STAGE.INFRASTRUCTURE");
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetService() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    StepParameters stepParameters = DummyStepParameters.builder().build();
    Ambiance ambiance = Ambiance.builder().build();
    ServiceOutcome serviceOutcome = ServiceOutcome.builder().build();

    StepDependencyResolverContext resolverContext =
        CDStepDependencyUtils.getStepDependencyResolverContext(inputPackage, stepParameters, ambiance);

    doReturn(Optional.of(serviceOutcome)).when(dependencyService).resolve(spec, resolverContext);
    ServiceOutcome service =
        CDStepDependencyUtils.getService(dependencyService, spec, inputPackage, stepParameters, ambiance);
    assertThat(service).isEqualTo(serviceOutcome);

    doReturn(Optional.empty()).when(dependencyService).resolve(spec, resolverContext);
    assertThatThrownBy(
        () -> CDStepDependencyUtils.getService(dependencyService, spec, inputPackage, stepParameters, ambiance))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetInfrastructure() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    StepParameters stepParameters = DummyStepParameters.builder().build();
    Ambiance ambiance = Ambiance.builder().build();
    InfrastructureOutcome infrastructureOutcome = K8sDirectInfrastructureOutcome.builder().build();

    StepDependencyResolverContext resolverContext =
        CDStepDependencyUtils.getStepDependencyResolverContext(inputPackage, stepParameters, ambiance);

    doReturn(Optional.of(infrastructureOutcome)).when(dependencyService).resolve(spec, resolverContext);
    InfrastructureOutcome infrastructure =
        CDStepDependencyUtils.getInfrastructure(dependencyService, spec, inputPackage, stepParameters, ambiance);
    assertThat(infrastructure).isEqualTo(infrastructureOutcome);

    doReturn(Optional.empty()).when(dependencyService).resolve(spec, resolverContext);
    assertThatThrownBy(
        () -> CDStepDependencyUtils.getInfrastructure(dependencyService, spec, inputPackage, stepParameters, ambiance))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Test
  @Owner(developers = ARCHIT)
  @Category(UnitTests.class)
  public void testGetK8sRolling() {
    StepDependencySpec spec = StepDependencySpec.defaultBuilder().key("TEST").build();
    StepInputPackage inputPackage = StepInputPackage.builder().build();
    StepParameters stepParameters = DummyStepParameters.builder().build();
    Ambiance ambiance = Ambiance.builder().build();
    K8sRollingOutcome k8sRollingOutcome = K8sRollingOutcome.builder().build();

    StepDependencyResolverContext resolverContext =
        CDStepDependencyUtils.getStepDependencyResolverContext(inputPackage, stepParameters, ambiance);

    doReturn(Optional.of(k8sRollingOutcome)).when(dependencyService).resolve(spec, resolverContext);
    K8sRollingOutcome k8sRolling =
        CDStepDependencyUtils.getK8sRolling(dependencyService, spec, inputPackage, stepParameters, ambiance);
    assertThat(k8sRolling).isEqualTo(k8sRollingOutcome);

    doReturn(Optional.empty()).when(dependencyService).resolve(spec, resolverContext);
    assertThatThrownBy(
        () -> CDStepDependencyUtils.getInfrastructure(dependencyService, spec, inputPackage, stepParameters, ambiance))
        .isInstanceOf(InvalidArgumentsException.class);
  }

  @Data
  @AllArgsConstructor
  public static class DummyOutcome implements Outcome {
    String name;
  }
}