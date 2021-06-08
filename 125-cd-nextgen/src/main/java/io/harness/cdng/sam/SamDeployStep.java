package io.harness.cdng.sam;

import io.harness.cdng.infra.beans.InfrastructureOutcome;
import io.harness.cdng.infra.yaml.AwsSamInfrastructure;
import io.harness.cdng.k8s.K8sStepHelper;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.steps.ManifestsOutcome;
import io.harness.cdng.manifest.yaml.ManifestOutcome;
import io.harness.cdng.manifest.yaml.kinds.AwsSamManifest;
import io.harness.cdng.provision.terraform.TerraformStepHelper;
import io.harness.cdng.stepsdependency.constants.OutcomeExpressionConstants;
import io.harness.connector.ConnectorInfoDTO;
import io.harness.connector.helper.EncryptionHelper;
import io.harness.delegate.beans.TaskData;
import io.harness.delegate.beans.connector.ConnectorConfigDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsConnectorDTO;
import io.harness.delegate.beans.connector.awsconnector.AwsManualConfigSpecDTO;
import io.harness.delegate.task.aws.AwsSamCommandUnit;
import io.harness.delegate.task.aws.AwsSamTaskNGResponse;
import io.harness.delegate.task.aws.AwsSamTaskParameters;
import io.harness.delegate.task.aws.AwsSamTaskType;
import io.harness.delegate.task.git.GitFetchFilesConfig;
import io.harness.delegate.task.terraform.TerraformTaskNGResponse;
import io.harness.executions.steps.ExecutionNodeType;
import io.harness.logging.CommandExecutionStatus;
import io.harness.logging.UnitProgress;
import io.harness.ngpipeline.common.ParameterFieldHelper;
import io.harness.plancreator.steps.common.StepElementParameters;
import io.harness.plancreator.steps.common.rollback.TaskExecutableWithRollback;
import io.harness.pms.contracts.ambiance.Ambiance;
import io.harness.pms.contracts.execution.tasks.TaskRequest;
import io.harness.pms.contracts.steps.StepType;
import io.harness.pms.execution.utils.AmbianceUtils;
import io.harness.pms.sdk.core.resolver.RefObjectUtils;
import io.harness.pms.sdk.core.resolver.outcome.OutcomeService;
import io.harness.pms.sdk.core.steps.io.StepInputPackage;
import io.harness.pms.sdk.core.steps.io.StepResponse;
import io.harness.provision.TerraformConstants;
import io.harness.security.encryption.EncryptedDataDetail;
import io.harness.serializer.KryoSerializer;
import io.harness.steps.StepUtils;
import io.harness.supplier.ThrowingSupplier;
import io.harness.yaml.core.variables.NGVariable;
import io.harness.yaml.core.variables.SecretNGVariable;

import software.wings.beans.TaskType;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SamDeployStep extends TaskExecutableWithRollback<AwsSamTaskNGResponse> {
  public static final StepType STEP_TYPE =
      StepType.newBuilder().setType(ExecutionNodeType.AWS_SAM_DEPLOY.getYamlType()).build();

  @Inject private OutcomeService outcomeService;
  @Inject private K8sStepHelper k8sStepHelper;
  @Inject private TerraformStepHelper tfStepHelper;
  @Inject private EncryptionHelper encryptionHelper;
  @Inject private KryoSerializer kryoSerializer;

  @Override
  public Class<StepElementParameters> getStepParametersClass() {
    return StepElementParameters.class;
  }

  @Override
  public TaskRequest obtainTask(
      Ambiance ambiance, StepElementParameters stepParameters, StepInputPackage inputPackage) {
    ManifestsOutcome manifestsOutcome = (ManifestsOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.MANIFESTS));

    InfrastructureOutcome infrastructureOutcome = (InfrastructureOutcome) outcomeService.resolve(
        ambiance, RefObjectUtils.getOutcomeRefObject(OutcomeExpressionConstants.INFRASTRUCTURE_OUTCOME));

    SamDeployStepInfo stepParametersSpec = (SamDeployStepInfo) stepParameters.getSpec();

    AwsSamManifest awsSamManifest =
        (AwsSamManifest) manifestsOutcome.values()
            .stream()
            .filter(manifestOutcome -> ManifestType.AwsSamManifest.equals(manifestOutcome.getType()))
            .collect(Collectors.toList())
            .get(0);

    AwsSamInfrastructure awsSamInfrastructure = (AwsSamInfrastructure) infrastructureOutcome;
    AwsConnectorDTO awsConnectorDTO =
        (AwsConnectorDTO) k8sStepHelper.getConnector(awsSamInfrastructure.getConnectorRef(), ambiance)
            .getConnectorConfig();

    List<EncryptedDataDetail> awsConnectorEncryptionDetails = encryptionHelper.getEncryptionDetail(
        ((AwsManualConfigSpecDTO) awsConnectorDTO.getCredential().getConfig()), AmbianceUtils.getAccountId(ambiance),
        AmbianceUtils.getOrgIdentifier(ambiance), AmbianceUtils.getProjectIdentifier(ambiance));
    GitFetchFilesConfig manifestsConfig =
        tfStepHelper.getGitFetchFilesConfig(awsSamManifest.getStoreConfig(), ambiance, awsSamManifest.getIdentifier());

    List<NGVariable> overrides = stepParametersSpec.getOverrides();
    Map<String, String> overridesMap = new HashMap<>();
    if (overrides != null) {
      for (NGVariable variable : overrides) {
        if (variable instanceof SecretNGVariable) {
          SecretNGVariable secretNGVariable = (SecretNGVariable) variable;
          String secretValue = secretNGVariable.getValue().getValue() != null
              ? secretNGVariable.getValue().getValue().toSecretRefStringValue()
              : secretNGVariable.getValue().getExpressionValue();
          String value = "${ngSecretManager.obtain(\"" + secretValue + "\", " + 0L + ")}";
          overridesMap.put(variable.getName(), value);
        }
        overridesMap.put(variable.getName(), (String) variable.getCurrentValue().getValue());
      }
    }

    AwsSamTaskParameters awsSamTaskParameters =
        AwsSamTaskParameters.builder()
            .accountId(AmbianceUtils.getAccountId(ambiance))
            .awsConnectorDTO(awsConnectorDTO)
            .awsConnectorEncryptionDetails(awsConnectorEncryptionDetails)
            .awsSamCommandUnit(AwsSamCommandUnit.Deploy)
            .awsSamTaskType(AwsSamTaskType.DEPLOY)
            .configFile(manifestsConfig)
            .globalAdditionalFlags(stepParametersSpec.getGlobalAdditionalFlags())
            .overrides(overridesMap)
            .region(stepParametersSpec.getRegion())
            .stackName(stepParametersSpec.getStackName())
            .timeoutInMillis(
                StepUtils.getTimeoutMillis(stepParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .s3BucketName(stepParametersSpec.s3BucketName)
            .build();

    TaskData taskData =
        TaskData.builder()
            .async(true)
            .taskType(TaskType.AWS_SAM_TASK_NG.name())
            .timeout(StepUtils.getTimeoutMillis(stepParameters.getTimeout(), TerraformConstants.DEFAULT_TIMEOUT))
            .parameters(new Object[] {awsSamTaskParameters})
            .build();

    return StepUtils.prepareTaskRequestWithTaskSelector(ambiance, taskData, kryoSerializer,
        Collections.singletonList(AwsSamCommandUnit.Deploy.name()), TaskType.AWS_SAM_TASK_NG.getDisplayName(),
        StepUtils.getTaskSelectors(stepParametersSpec.getDelegateSelectors()));
  }

  @Override
  public StepResponse handleTaskResult(Ambiance ambiance, StepElementParameters stepParameters,
      ThrowingSupplier<AwsSamTaskNGResponse> responseDataSupplier) throws Exception {
    StepResponse.StepResponseBuilder stepResponseBuilder = StepResponse.builder();

    AwsSamTaskNGResponse taskResponse = responseDataSupplier.get();
    List<UnitProgress> unitProgresses = taskResponse.getUnitProgressData() == null
        ? Collections.emptyList()
        : taskResponse.getUnitProgressData().getUnitProgresses();
    stepResponseBuilder.unitProgressList(unitProgresses)
        .status(StepUtils.getStepStatus(taskResponse.getCommandExecutionStatus()));

    if (CommandExecutionStatus.SUCCESS == taskResponse.getCommandExecutionStatus()) {
      //      addStepOutcomeToStepResponse(stepResponseBuilder, taskResponse);
    }
    return stepResponseBuilder.build();
  }
}
