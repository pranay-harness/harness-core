package io.harness.functional.trigger;

import static io.harness.persistence.HQuery.excludeAuthority;
import static org.assertj.core.api.Assertions.assertThat;

import com.google.inject.Inject;

import io.harness.functional.AbstractFunctionalTest;
import io.harness.generator.ApplicationGenerator;
import io.harness.generator.ApplicationGenerator.Applications;
import io.harness.generator.OwnerManager;
import io.harness.generator.OwnerManager.Owners;
import io.harness.generator.Randomizer.Seed;
import io.harness.generator.ServiceGenerator;
import io.harness.generator.ServiceGenerator.Services;
import io.harness.generator.WorkflowGenerator;
import io.harness.generator.WorkflowGenerator.Workflows;
import io.harness.generator.artifactstream.ArtifactStreamManager;
import io.harness.generator.artifactstream.ArtifactStreamManager.ArtifactStreams;
import io.harness.rest.RestResponse;
import io.harness.testframework.framework.Setup;
import io.restassured.http.ContentType;
import io.restassured.mapper.ObjectMapperType;
import org.junit.Before;
import software.wings.beans.Application;
import software.wings.beans.FeatureFlag;
import software.wings.beans.FeatureFlag.FeatureFlagKeys;
import software.wings.beans.FeatureName;
import software.wings.beans.Service;
import software.wings.beans.Workflow;
import software.wings.beans.artifact.ArtifactStream;
import software.wings.beans.trigger.DeploymentTrigger;
import software.wings.dl.WingsPersistence;
import software.wings.service.impl.WorkflowExecutionServiceImpl;
import software.wings.service.impl.trigger.DeploymentTriggerServiceImpl;

import javax.ws.rs.core.GenericType;

public class AbstractTriggerFunctionalTestHelper extends AbstractFunctionalTest {
  @Inject protected OwnerManager ownerManager;
  @Inject protected ApplicationGenerator applicationGenerator;
  @Inject protected WorkflowGenerator workflowGenerator;
  @Inject protected ServiceGenerator serviceGenerator;
  @Inject protected ArtifactStreamManager artifactStreamManager;
  @Inject protected DeploymentTriggerServiceImpl deploymentTriggerService;
  @Inject protected WorkflowExecutionServiceImpl workflowExecutionService;
  @Inject WingsPersistence wingsPersistence;

  Application application;

  GenericType<RestResponse<DeploymentTrigger>> triggerType = new GenericType<RestResponse<DeploymentTrigger>>() {

  };

  final Seed seed = new Seed(0);
  Owners owners;
  Workflow buildWorkflow;
  ArtifactStream artifactStream;
  Service service;
  @Before
  public void setUp() {
    owners = ownerManager.create();
    application = applicationGenerator.ensurePredefined(seed, owners, Applications.GENERIC_TEST);
    assertThat(application).isNotNull();

    wingsPersistence.update(wingsPersistence.createQuery(FeatureFlag.class, excludeAuthority)
                                .filter(FeatureFlagKeys.name, FeatureName.TRIGGER_REFACTOR),
        wingsPersistence.createUpdateOperations(FeatureFlag.class).set("enabled", true));

    service = owners.obtainService(() -> serviceGenerator.ensurePredefined(seed, owners, Services.GENERIC_TEST));
    assertThat(service).isNotNull();
    artifactStream = artifactStreamManager.ensurePredefined(seed, owners, ArtifactStreams.HARNESS_SAMPLE_ECHO_WAR);
    assertThat(artifactStream).isNotNull();
    buildWorkflow = workflowGenerator.ensurePredefined(seed, owners, Workflows.BUILD_JENKINS);
    assertThat(buildWorkflow).isNotNull();
  }

  public DeploymentTrigger saveAndGetTrigger(DeploymentTrigger trigger) {
    RestResponse<DeploymentTrigger> savedTriggerResponse = Setup.portal()
                                                               .auth()
                                                               .oauth2(bearerToken)
                                                               .queryParam("accountId", application.getAccountId())
                                                               .queryParam("appId", application.getUuid())
                                                               .body(trigger, ObjectMapperType.GSON)
                                                               .contentType(ContentType.JSON)
                                                               .post("/deployment-triggers")
                                                               .as(triggerType.getType());

    return savedTriggerResponse.getResource();
  }

  public DeploymentTrigger getDeploymentTrigger(String uuId, String appId) {
    RestResponse<DeploymentTrigger> savedTriggerResponse = Setup.portal()
                                                               .auth()
                                                               .oauth2(bearerToken)
                                                               .queryParam("appId", appId)
                                                               .pathParam("triggerId", uuId)
                                                               .get("/deployment-triggers/{triggerId}")
                                                               .as(triggerType.getType());

    return savedTriggerResponse.getResource();
  }

  public void deleteTrigger(String uuId, String appId) {
    Setup.portal()
        .auth()
        .oauth2(bearerToken)
        .queryParam("appId", appId)
        .pathParam("triggerId", uuId)
        .delete("/deployment-triggers/{triggerId}")
        .then()
        .statusCode(200);
  }
}
