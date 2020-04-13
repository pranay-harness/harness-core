package software.wings.sm.states;

import static software.wings.common.Constants.DEFAULT_STEADY_STATE_TIMEOUT;

import com.github.reinert.jjschema.Attributes;
import com.github.reinert.jjschema.SchemaIgnore;
import io.harness.context.ContextElementType;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.api.HelmDeployContextElement;
import software.wings.api.HelmDeployStateExecutionData;
import software.wings.beans.Application;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ApplicationManifest;
import software.wings.beans.artifact.Artifact;
import software.wings.beans.command.CommandUnit;
import software.wings.beans.command.HelmDummyCommandUnit;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.container.ImageDetails;
import software.wings.helpers.ext.helm.HelmConstants.HelmVersion;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;
import software.wings.helpers.ext.helm.request.HelmRollbackCommandRequest;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;
import software.wings.helpers.ext.k8s.request.K8sValuesLocation;
import software.wings.service.impl.ContainerServiceParams;
import software.wings.sm.ContextElement;
import software.wings.sm.ExecutionContext;
import software.wings.sm.StateType;
import software.wings.stencils.DefaultValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
      String accountId, String appId, String activityId, ImageDetails imageTag, String repoName, GitConfig gitConfig,
      List<EncryptedDataDetail> encryptedDataDetails, String commandFlags, K8sDelegateManifestConfig repoConfig,
      Map<K8sValuesLocation, ApplicationManifest> appManifestMap, HelmVersion helmVersion) {
    Integer previousReleaseRevision = null;

    ContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
    if (contextElement != null) {
      previousReleaseRevision = ((HelmDeployContextElement) contextElement).getPreviousReleaseRevision();
    }

    long steadyStateTimeout =
        getSteadyStateTimeout() > 0 ? getSteadyStateTimeout() : DEFAULT_STEADY_STATE_TIMEOUT; // 10 minutes if not set
    return HelmRollbackCommandRequest.builder()
        .releaseName(releaseName)
        .prevReleaseVersion(previousReleaseRevision != null ? previousReleaseRevision : -1)
        .accountId(accountId)
        .appId(context.getAppId())
        .activityId(activityId)
        .commandName(HELM_COMMAND_NAME)
        .timeoutInMillis(getTimeout(steadyStateTimeout))
        .containerServiceParams(containerServiceParams)
        .chartSpecification(helmChartSpecification)
        .repoName(repoName)
        .gitConfig(gitConfig)
        .encryptedDataDetails(encryptedDataDetails)
        .commandFlags(commandFlags)
        .sourceRepoConfig(repoConfig)
        .helmVersion(helmVersion)
        .build();
  }

  @Override
  protected ImageDetails getImageDetails(ExecutionContext context, Artifact artifact) {
    return null;
  }

  @Override
  protected void setNewAndPrevReleaseVersion(ExecutionContext context, Application app, String releaseName,
      ContainerServiceParams containerServiceParams, HelmDeployStateExecutionData stateExecutionData,
      GitConfig gitConfig, List<EncryptedDataDetail> encryptedDataDetails, String commandFlags,
      HelmVersion helmVersion) {
    HelmDeployContextElement contextElement = context.getContextElement(ContextElementType.HELM_DEPLOY);
    if (contextElement != null) {
      stateExecutionData.setReleaseOldVersion(contextElement.getNewReleaseRevision());
      stateExecutionData.setReleaseNewVersion(contextElement.getNewReleaseRevision() + 1);
      stateExecutionData.setRollbackVersion(contextElement.getPreviousReleaseRevision());
    }
  }

  @Override
  @Attributes(title = "Deployment steady state timeout (in minutes).")
  @DefaultValue("10")
  public int getSteadyStateTimeout() {
    return super.getSteadyStateTimeout();
  }

  @Override
  @SchemaIgnore
  public String getHelmReleaseNamePrefix() {
    return super.getHelmReleaseNamePrefix();
  }

  @Override
  @SchemaIgnore
  public GitFileConfig getGitFileConfig() {
    return super.getGitFileConfig();
  }

  @Override
  @SchemaIgnore
  public String getCommandFlags() {
    return super.getCommandFlags();
  }

  @Override
  public Map<String, String> validateFields() {
    return null;
  }

  @Override
  protected List<CommandUnit> getCommandUnits(boolean valuesInGit, boolean valuesInHelmChartRepo) {
    List<CommandUnit> commandUnits = new ArrayList<>();

    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.Init));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.Rollback));
    commandUnits.add(new HelmDummyCommandUnit(HelmDummyCommandUnit.WaitForSteadyState));

    return commandUnits;
  }
}
