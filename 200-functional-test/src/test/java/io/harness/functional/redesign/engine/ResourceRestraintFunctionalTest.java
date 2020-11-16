package io.harness.functional.redesign.engine;

import static io.harness.data.structure.UUIDGenerator.generateUuid;
import static io.harness.pms.execution.Status.SUCCEEDED;
import static io.harness.rule.OwnerRule.ALEXEI;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.StatusUtils;
import io.harness.adviser.AdviserObtainment;
import io.harness.adviser.OrchestrationAdviserTypes;
import io.harness.advisers.success.OnSuccessAdviserParameters;
import io.harness.beans.EmbeddedUser;
import io.harness.category.element.FunctionalTests;
import io.harness.execution.PlanExecution;
import io.harness.facilitator.FacilitatorObtainment;
import io.harness.facilitator.FacilitatorType;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.redesign.OrchestrationEngineTestSetupHelper;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.Randomizer;
import io.harness.generator.ResourceConstraintGenerator;
import io.harness.generator.ResourceConstraintGenerator.ResourceConstraints;
import io.harness.plan.Plan;
import io.harness.plan.PlanNode;
import io.harness.pms.advisers.AdviserType;
import io.harness.redesign.services.CustomExecutionService;
import io.harness.rule.Owner;
import io.harness.steps.dummy.DummyStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStep;
import io.harness.steps.resourcerestraint.ResourceRestraintStepParameters;
import io.harness.steps.resourcerestraint.beans.AcquireMode;
import io.harness.steps.resourcerestraint.beans.HoldingScope.HoldingScopeBuilder;
import io.harness.steps.resourcerestraint.beans.ResourceConstraint;
import org.awaitility.Awaitility;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Application;

import java.util.concurrent.TimeUnit;

public class ResourceRestraintFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private OrchestrationEngineTestSetupHelper engineTestSetupHelper;
  @Inject private ApplicationGenerator applicationGenerator;
  @Inject private CustomExecutionService customExecutionService;
  @Inject private ResourceConstraintGenerator resourceConstraintGenerator;

  private ResourceConstraint resourceConstraint;

  OwnerManager.Owners owners;
  Application application;

  final Randomizer.Seed seed = new Randomizer.Seed(0);

  @Before
  public void setUp() {
    owners = ownerManager.create();
    owners.add(EmbeddedUser.builder().uuid(generateUuid()).name(ALEXEI).email(ALEXEI).build());
    application = applicationGenerator.ensurePredefined(seed, owners, ApplicationGenerator.Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    resourceConstraint =
        resourceConstraintGenerator.ensurePredefined(seed, owners, ResourceConstraints.GENERIC_FIFO_TEST);
    assertThat(resourceConstraint).isNotNull();
  }

  @Test
  @Owner(developers = ALEXEI)
  @Category(FunctionalTests.class)
  public void shouldExecuteResourceRestraint() {
    PlanExecution original = customExecutionService.executeResourceRestraintPlanForFunctionalTest(
        provideResourceRestraintPlan(), owners.obtainUser());

    Awaitility.await().atMost(5, TimeUnit.MINUTES).pollInterval(10, TimeUnit.SECONDS).until(() -> {
      final PlanExecution planExecution = engineTestSetupHelper.getPlanExecution(original.getUuid());
      return planExecution != null && StatusUtils.finalStatuses().contains(planExecution.getStatus());
    });

    assertThat(engineTestSetupHelper.getPlanExecution(original.getUuid()).getStatus()).isEqualTo(SUCCEEDED);
  }

  private Plan provideResourceRestraintPlan() {
    String dummyNode1Id = generateUuid();
    String dummyNode2Id = generateUuid();
    String resourceRestraintInstanceId = generateUuid();
    String complaintId = "kmpySmUISimoRrJL6NL73w";
    return Plan.builder()
        .startingNodeId(dummyNode1Id)
        .node(PlanNode.builder()
                  .uuid(dummyNode1Id)
                  .name("Dummy Node 1")
                  .stepType(DummyStep.STEP_TYPE)
                  .identifier("dummy1")
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                          .parameters(
                              OnSuccessAdviserParameters.builder().nextNodeId(resourceRestraintInstanceId).build())
                          .build())
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(resourceRestraintInstanceId)
                  .identifier("resourceRestraint1")
                  .name("resourceRestraint1")
                  .stepType(ResourceRestraintStep.STEP_TYPE)
                  .stepParameters(ResourceRestraintStepParameters.builder()
                                      .claimantId(complaintId)
                                      .permits(1)
                                      .resourceUnit(generateUuid())
                                      .resourceRestraintId(resourceConstraint.getUuid())
                                      .acquireMode(AcquireMode.ACCUMULATE)
                                      .holdingScope(HoldingScopeBuilder.aPlan().build())
                                      .build())
                  .adviserObtainment(
                      AdviserObtainment.builder()
                          .type(AdviserType.newBuilder().setType(OrchestrationAdviserTypes.ON_SUCCESS.name()).build())
                          .parameters(OnSuccessAdviserParameters.builder().nextNodeId(dummyNode2Id).build())
                          .build())
                  .facilitatorObtainment(
                      FacilitatorObtainment.builder()
                          .type(FacilitatorType.builder().type(FacilitatorType.RESOURCE_RESTRAINT).build())
                          .build())
                  .build())
        .node(PlanNode.builder()
                  .uuid(dummyNode2Id)
                  .name("Dummy Node 2")
                  .stepType(DummyStep.STEP_TYPE)
                  .identifier("dummy2")
                  .facilitatorObtainment(FacilitatorObtainment.builder()
                                             .type(FacilitatorType.builder().type(FacilitatorType.SYNC).build())
                                             .build())
                  .build())
        .build();
  }
}
