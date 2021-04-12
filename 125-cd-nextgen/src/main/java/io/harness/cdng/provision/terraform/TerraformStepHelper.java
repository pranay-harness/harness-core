package io.harness.cdng.provision.terraform;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.yaml.StoreConfig;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.exception.InvalidRequestException;
import io.harness.ngpipeline.common.AmbianceHelper;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.sdk.core.data.OptionalSweepingOutput;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outputs.ExecutionSweepingOutputService;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(CDP)
@Slf4j
@Singleton
public class TerraformStepHelper {
  private static final String INHERIT_OUTPUT_FORMAT = "tfInheritOutput_%s";

  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private ExecutionSweepingOutputService executionSweepingOutputService;

  public String generateFullIdentifier(String provisionerIdentifier, Ambiance ambiance) {
    return String.format("%s/%s/%s/%s", AmbianceHelper.getAccountId(ambiance),
        AmbianceHelper.getOrgIdentifier(ambiance), AmbianceHelper.getProjectIdentifier(ambiance),
        provisionerIdentifier);
  }

  public GitFetchFilesConfig gitFetchFilesConfig(
      StoreConfig store, Ambiance ambiance, String identifier, String manifestType) {
    String vaidationMessage = String.format("Invalid type for manifestType: [%s]", manifestType);
    return k8sStepHelper.getGitFetchFilesConfig(ambiance, identifier, store, vaidationMessage, manifestType);
  }

  public TerraformInheritOutput getSavedInheritOutput(String provisionerIdentifier, Ambiance ambiance) {
    String fullEntityId = generateFullIdentifier(provisionerIdentifier, ambiance);
    String inheritOutputName = String.format(INHERIT_OUTPUT_FORMAT, fullEntityId);
    OptionalSweepingOutput output = executionSweepingOutputService.resolveOptional(
        ambiance, RefObjectUtils.getSweepingOutputRefObject(inheritOutputName));
    if (!output.isFound()) {
      throw new InvalidRequestException(String.format("Terraform inherit output: [%s] not found", inheritOutputName));
    }

    return (TerraformInheritOutput) output.getOutput();
  }
}
