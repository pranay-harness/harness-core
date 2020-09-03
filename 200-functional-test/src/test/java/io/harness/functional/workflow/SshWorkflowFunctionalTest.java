package io.harness.functional.workflow;

import static io.harness.beans.ExecutionStatus.SUCCESS;
import static io.harness.rule.OwnerRule.YOGESH;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.InfrastructureDefinition;

import java.util.Collections;

public class SshWorkflowFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private WorkflowUtils workflowUtils;

  @Inject private ServiceGenerator serviceGenerator;
  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  private final Randomizer.Seed seed = new Randomizer.Seed(0);

  private Owners owners;
  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
    service = serviceGenerator.ensureGenericTest(seed, owners, "k8s-service");
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  public void shouldRunSshWorkflowWithOneNoNodePhase() {
    final String appId = service.getAppId();
    final String accountId = service.getAccountId();

    resetCache(accountId);

    InfrastructureDefinition infraDef = infrastructureDefinitionGenerator.ensurePredefined(
        seed, owners, InfrastructureDefinitionGenerator.InfrastructureDefinitions.PHYSICAL_SSH_TEST);
    Workflow workflow = workflowUtils.createMultiPhaseSshWorkflowWithNoNodePhase("ssh-pdc-rolling", service, infraDef);
    workflow = workflowGenerator.ensureWorkflow(seed, owners, workflow);
    WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, appId, null, workflow.getUuid(), Collections.<Artifact>emptyList());
    assertThat(workflowExecution.getStatus()).isEqualTo(SUCCESS);
  }
}
