package io.harness.functional.workflow;

import static io.harness.rule.OwnerRule.YOGESH_CHAUHAN;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.OwnerRule.Owner;
import io.harness.testframework.restutils.ArtifactRestUtils;
import io.harness.testframework.restutils.InfrastructureDefinitionRestUtils;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.beans.AwsAmiInfrastructureMapping;
import software.wings.beans.InfrastructureMapping;
import software.wings.beans.InfrastructureType;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.Artifact;
import software.wings.infra.AwsAmiInfrastructure;
import software.wings.infra.InfrastructureDefinition;
import software.wings.service.impl.aws.model.AwsAsgGetRunningCountData;
import software.wings.service.intfc.InfrastructureMappingService;

import java.util.List;

public class AmiWorkflowFunctionalTest extends AbstractFunctionalTest {
  @Inject private OwnerManager ownerManager;
  @Inject private ServiceGenerator serviceGenerator;
  @Inject private InfrastructureDefinitionGenerator infrastructureDefinitionGenerator;
  @Inject private InfrastructureMappingService infrastructureMappingService;

  @Inject private WorkflowGenerator workflowGenerator;
  @Inject private WorkflowUtils workflowUtils;

  private final Seed seed = new Seed(0);
  private Owners owners;

  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Owner(developers = YOGESH_CHAUHAN)
  @Category(FunctionalTests.class)
  @Ignore("Enable once feature flag is enabled for infra refactor")
  public void shouldRunAwsAmiWorkflow() {
    service = serviceGenerator.ensureAmiGenericTest(seed, owners, "aws-ami");
    final String accountId = service.getAccountId();
    final String appId = service.getAppId();

    resetCache(service.getAccountId());

    InfrastructureDefinition amiInfrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureType.AWS_AMI, bearerToken);

    final String envId = amiInfrastructureDefinition.getEnvId();

    Workflow bgWorkflow = workflowUtils.createAwsAmiBGWorkflow("ami-bg-", service, amiInfrastructureDefinition);

    bgWorkflow = workflowGenerator.ensureWorkflow(seed, owners, bgWorkflow);

    resetCache(service.getAccountId());

    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0));

    AwsAsgGetRunningCountData runningCountData = InfrastructureDefinitionRestUtils.amiRunningInstances(
        bearerToken, accountId, appId, service.getUuid(), amiInfrastructureDefinition.getUuid());

    Assertions.assertThat(runningCountData).isNotNull();

    runWorkflow(bearerToken, appId, envId, bgWorkflow.getUuid(), ImmutableList.of(artifact));

    List<InfrastructureMapping> infrastructureMappings =
        infrastructureMappingService.getInfraMappingLinkedToInfraDefinition(
            appId, amiInfrastructureDefinition.getUuid());

    Assertions.assertThat(infrastructureMappings).hasSize(1);
    AwsAmiInfrastructureMapping amiInfraMapping = (AwsAmiInfrastructureMapping) infrastructureMappings.get(0);
    AwsAmiInfrastructure amiInfrastructure = (AwsAmiInfrastructure) amiInfrastructureDefinition.getInfrastructure();
    Assertions.assertThat(amiInfraMapping.getRegion()).isEqualTo(amiInfrastructure.getRegion());
    Assertions.assertThat(amiInfraMapping.getAutoScalingGroupName())
        .isEqualTo(amiInfrastructure.getAutoScalingGroupName());
    Assertions.assertThat(amiInfraMapping.getClassicLoadBalancers())
        .isEqualTo(amiInfrastructure.getClassicLoadBalancers());
    Assertions.assertThat(amiInfraMapping.getTargetGroupArns()).isEqualTo(amiInfrastructure.getTargetGroupArns());
    Assertions.assertThat(amiInfraMapping.getHostNameConvention()).isEqualTo(amiInfrastructure.getHostNameConvention());
    Assertions.assertThat(amiInfraMapping.getStageClassicLoadBalancers())
        .isEqualTo(amiInfrastructure.getStageClassicLoadBalancers());
    Assertions.assertThat(amiInfraMapping.getStageTargetGroupArns())
        .isEqualTo(amiInfrastructure.getStageTargetGroupArns());
    Assertions.assertThat(amiInfraMapping.getAmiDeploymentType()).isEqualTo(amiInfrastructure.getAmiDeploymentType());
    Assertions.assertThat(amiInfraMapping.getSpotinstCloudProvider())
        .isEqualTo(amiInfrastructure.getSpotinstCloudProvider());
    Assertions.assertThat(amiInfraMapping.getSpotinstElastiGroupJson())
        .isEqualTo(amiInfrastructure.getSpotinstElastiGroupJson());

    //    Assertions.assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    // TODO: delete ASG
  }
}
