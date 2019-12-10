package io.harness.functional.workflow;

import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;

import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.OwnerRule.Owner;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;

import java.util.Collections;

public class BuildWorkflowFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowUtils workflowUtils;

  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  private final Seed seed = new Seed(0);

  private Owners owners;
  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    service = serviceGenerator.ensureK8sTest(seed, owners, "k8s-service");
  }

  @Test
  @Owner(developers = YOGESH_CHAUHAN, intermittent = true)
  @Category(FunctionalTests.class)
  public void shouldRunBuildWorkflow() {
    final String appId = service.getAppId();
    final String accountId = service.getAccountId();

    resetCache(accountId);

    Workflow buildWorkflow =
        workflowUtils.createBuildWorkflow("build-workflow-", appId, service.getArtifactStreamIds());
    buildWorkflow = workflowGenerator.ensureWorkflow(seed, owners, buildWorkflow);
    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, appId, null, buildWorkflow.getUuid(), Collections.<Artifact>emptyList());
    Assertions.assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
  }
}
