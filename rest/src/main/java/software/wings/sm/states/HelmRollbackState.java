package software.wings.sm.states;

import software.wings.api.HelmDeployContextElement;
import software.wings.beans.ContainerInfrastructureMapping;
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

import java.util.ArrayList;
import java.util.List;

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
    String previousReleaseRevision = null;

    ContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
    if (contextElement != null) {
      previousReleaseRevision = ((HelmDeployContextElement) contextElement).getPreviousReleaseRevision();
    }

    return HelmRollbackCommandRequest.builder()
        .releaseName(releaseName)
        .revision(previousReleaseRevision)
        .accountId(accountId)
        .appId(context.getAppId())
        .activityId(activityId)
        .commandName(HELM_COMMAND_NAME)
        .containerServiceParams(containerServiceParams)
        .build();
  }

  @Override
  protected List<InstanceStatusSummary> getInstanceStatusSummaries(
      ExecutionContext context, HelmCommandExecutionResponse executionResponse) {
    return new ArrayList<>();
  }
}
