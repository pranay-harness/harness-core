package io.harness.delegate.task.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;
import static io.harness.expression.Expression.DISALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.logstreaming.CommandUnitsProgress;
import io.harness.expression.Expression;

import java.util.List;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@OwnedBy(CDP)
public class K8sRollingDeployRequest implements K8sDeployRequest {
  boolean skipDryRun;
  @Expression(DISALLOW_SECRETS) String releaseName;
  String commandName;
  K8sTaskType taskType;
  Integer timeoutIntervalInMin;
  @Expression(ALLOW_SECRETS) List<String> valuesYamlList;
  @Expression(ALLOW_SECRETS) List<String> openshiftParamList;
  @Expression(ALLOW_SECRETS) List<String> kustomizePatchesList;
  K8sInfraDelegateConfig k8sInfraDelegateConfig;
  ManifestDelegateConfig manifestDelegateConfig;
  boolean inCanaryWorkflow;
  boolean localOverrideFeatureFlag;
  String accountId;
  boolean deprecateFabric8Enabled;
  boolean skipResourceVersioning;
  @Builder.Default boolean shouldOpenFetchFilesLogStream = true;
  CommandUnitsProgress commandUnitsProgress;
  boolean useVarSupportForKustomize;
  boolean useNewKubectlVersion;
  boolean pruningEnabled;
}
