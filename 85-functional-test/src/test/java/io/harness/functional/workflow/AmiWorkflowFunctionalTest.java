package io.harness.functional.workflow;

import static io.harness.rule.OwnerRule.ROHIT_KUMAR;
import static io.harness.rule.OwnerRule.YOGESH;

import com.google.common.collect.ImmutableList;
import com.google.inject.Inject;

import io.harness.beans.ExecutionStatus;
import io.harness.category.element.FunctionalTests;
import io.harness.functional.AbstractFunctionalTest;
import io.harness.functional.WorkflowUtils;
import io.harness.generator.InfrastructureDefinitionGenerator;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.WorkflowGenerator;
import io.harness.rule.Owner;
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
import software.wings.beans.WorkflowExecution;
import software.wings.beans.artifact.Artifact;
import software.wings.dl.WingsPersistence;
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
  @Inject private WingsPersistence wingsPersistence;

  private final Seed seed = new Seed(0);
  private Owners owners;

  private Service service;

  @Before
  public void setUp() {
    owners = ownerManager.create();
  }

  @Test
  @Owner(developers = YOGESH)
  @Category(FunctionalTests.class)
  @Ignore("Enable once feature flag is enabled for infra refactor")
  public void shouldRunAwsAmiWorkflow() {
    service = serviceGenerator.ensureAmiGenericTest(seed, owners, "aws-ami");
    final String accountId = service.getAccountId();
    final String appId = service.getAppId();

    resetCache(service.getAccountId());

    InfrastructureDefinition amiInfrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureType.AWS_AMI, bearerToken);
    ensureInfraMapping(service, amiInfrastructureDefinition);
    final String envId = amiInfrastructureDefinition.getEnvId();

    Workflow bgWorkflow = workflowUtils.createAwsAmiBGWorkflow("ami-bg-", service, amiInfrastructureDefinition);

    bgWorkflow = workflowGenerator.ensureWorkflow(seed, owners, bgWorkflow);

    resetCache(service.getAccountId());

    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0));

    AwsAsgGetRunningCountData runningCountData = InfrastructureDefinitionRestUtils.amiRunningInstances(
        bearerToken, accountId, appId, service.getUuid(), amiInfrastructureDefinition.getUuid());

    Assertions.assertThat(runningCountData).isNotNull();

    final WorkflowExecution workflowExecution =
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

    Assertions.assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    // TODO: delete ASG
  }

  @Test
  @Owner(developers = ROHIT_KUMAR)
  @Category(FunctionalTests.class)
  @Ignore("Enable once feature flag is enabled for infra refactor")
  public void shouldRunAwsAmiWorkflow_Launchtemplate() {
    service = serviceGenerator.ensureAmiGenericTest(seed, owners, "aws-ami-lt");
    final String accountId = service.getAccountId();
    final String appId = service.getAppId();

    resetCache(accountId);

    InfrastructureDefinition amiInfrastructureDefinition =
        infrastructureDefinitionGenerator.ensurePredefined(seed, owners, InfrastructureType.AWS_AMI_LT, bearerToken);
    ensureInfraMapping(service, amiInfrastructureDefinition);

    final String envId = amiInfrastructureDefinition.getEnvId();

    Workflow bgWorkflow = workflowUtils.createAwsAmiBGWorkflow("ami-bg-lt-", service, amiInfrastructureDefinition);

    bgWorkflow = workflowGenerator.ensureWorkflow(seed, owners, bgWorkflow);

    resetCache(service.getAccountId());

    Artifact artifact = ArtifactRestUtils.waitAndFetchArtifactByArtfactStream(
        bearerToken, appId, service.getArtifactStreamIds().get(0));

    final WorkflowExecution workflowExecution =
        runWorkflow(bearerToken, appId, envId, bgWorkflow.getUuid(), ImmutableList.of(artifact));
    Assertions.assertThat(workflowExecution.getStatus()).isEqualTo(ExecutionStatus.SUCCESS);
    //  todo delete ASG
  }

  private String ensureInfraMapping(Service service, InfrastructureDefinition amiInfrastructureDefinition) {
    // ensure inframapping is always created with the same id, as the id is used for tagging resources
    final InfrastructureMapping infraMapping = amiInfrastructureDefinition.getInfraMapping();
    infraMapping.setName(amiInfrastructureDefinition.getName() + "_inframapping");
    infraMapping.setUuid(amiInfrastructureDefinition.getName() + "_inframapping_id");
    infraMapping.setServiceId(service.getUuid());
    infraMapping.setInfrastructureDefinitionId(amiInfrastructureDefinition.getUuid());
    infraMapping.setAccountId(service.getAccountId());
    return wingsPersistence.save(infraMapping);
  }
}
