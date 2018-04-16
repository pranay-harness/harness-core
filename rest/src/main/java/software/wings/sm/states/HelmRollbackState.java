package software.wings.sm.states;

import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;

import com.github.reinert.jjschema.Attributes;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.ContainerInfrastructureMapping;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.helm.HelmCommandExecutionResponse;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;
import software.wings.sm.InstanceStatusSummary;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 4/3/18.
 */
public class HelmRollbackState extends HelmDeployState {
  /**
   * Instantiates a new state.
   *
   * @param name the name
   */
  public HelmRollbackState(String name) {
    super(name, StateType.HELM_ROLLBACK.name());
  }

  @Override
  protected HelmCommandRequest getHelmCommandRequest(ExecutionContext context,
      HelmChartSpecification helmChartSpecification, ContainerServiceParams containerServiceParams, String releaseName,
      String accountId, String appId, String activityId, ImageDetails imageTag,
      ContainerInfrastructureMapping infrastructureMapping) {
    Integer previousReleaseRevision = null;

    ContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
    if (contextElement != null) {
      previousReleaseRevision = ((HelmDeployContextElement) contextElement).getPreviousReleaseRevision();
    }

    long steadyStateTimeout =
        getSteadyStateTimeout() > 0 ? getSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT; // 10 minutes if not set
    return HelmRollbackCommandRequest.builder()
        .releaseName(releaseName)
        .prevReleaseVersion(previousReleaseRevision)
        .accountId(accountId)
        .appId(context.getAppId())
        .activityId(activityId)
        .commandName(HELM_COMMAND_NAME)
        .timeoutInMillis(TimeUnit.MINUTES.toMillis(steadyStateTimeout))
        .containerServiceParams(containerServiceParams)
        .build();
  }

  @Override
  protected List<InstanceStatusSummary> getInstanceStatusSummaries(
      ExecutionContext context, HelmCommandExecutionResponse executionResponse) {
    return new ArrayList<>();
  }

  @Override
  protected ImageDetails getImageDetails(ExecutionContext context, Application app, Artifact artifact) {
    return null;
  }

  @Override
  protected void setNewAndPrevReleaseVersion(ExecutionContext context, Application app, String releaseName,
      ContainerServiceParams containerServiceParams, HelmDeployStateExecutionData stateExecutionData) {
    HelmDeployContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
    if (contextElement != null) {
      stateExecutionData.setReleaseOldVersion(contextElement.getNewReleaseRevision());
      stateExecutionData.setReleaseNewVersion(contextElement.getNewReleaseRevision() + 1);
      stateExecutionData.setRollbackVersion(contextElement.getPreviousReleaseRevision());
    }
  }

  @Attributes(title = "Deployment steady state timeout (in minutes).")
  @DefaultValue("10")
  public int getSteadyStateTimeout() {
    return super.getSteadyStateTimeout();
  }
}
