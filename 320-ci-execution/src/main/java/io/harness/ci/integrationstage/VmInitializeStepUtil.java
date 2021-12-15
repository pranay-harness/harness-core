package io.harness.ci.integrationstage;

import static io.harness.beans.serializer.RunTimeInputHandler.resolveStringParameter;
import static io.harness.common.CIExecutionConstants.STEP_WORK_DIR;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import io.harness.annotations.dev.*;
import io.harness.beans.environment.*;
import io.harness.beans.executionargs.*;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.stepinfo.*;
import io.harness.exception.ngexception.*;
import io.harness.plancreator.execution.*;
import io.harness.plancreator.stages.stage.*;
import io.harness.plancreator.steps.*;

import com.google.inject.Singleton;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.*;

@Singleton
@Slf4j
@OwnedBy(HarnessTeam.CI)
public class VmInitializeStepUtil {
  public BuildJobEnvInfo getInitializeStepInfoBuilder(StageElementConfig stageElementConfig,
      CIExecutionArgs ciExecutionArgs, List<ExecutionWrapperConfig> steps, String accountId) {
    ArrayList<String> connectorIdentifierList = new ArrayList<>();
    for (ExecutionWrapperConfig executionWrapper : steps) {
      if (executionWrapper.getStep() != null && !executionWrapper.getStep().isNull()) {
        StepElementConfig stepElementConfig = IntegrationStageUtils.getStepElementConfig(executionWrapper);
        extractConnectorIdentifier(connectorIdentifierList, stepElementConfig);
      } else if (executionWrapper.getParallel() != null && !executionWrapper.getParallel().isNull()) {
        ParallelStepElementConfig parallelStepElementConfig =
            IntegrationStageUtils.getParallelStepElementConfig(executionWrapper);
        if (isNotEmpty(parallelStepElementConfig.getSections())) {
          for (ExecutionWrapperConfig executionWrapperInParallel : parallelStepElementConfig.getSections()) {
            if (executionWrapperInParallel.getStep() == null || executionWrapperInParallel.getStep().isNull()) {
              continue;
            }
            StepElementConfig stepElementConfig =
                IntegrationStageUtils.getStepElementConfig(executionWrapperInParallel);
            extractConnectorIdentifier(connectorIdentifierList, stepElementConfig);
          }
        }
      }
    }
    return VmBuildJobInfo.builder()
        .ciExecutionArgs(ciExecutionArgs)
        .workDir(STEP_WORK_DIR)
        .connectorRefIdentifierList(connectorIdentifierList)
        .stageVars(stageElementConfig.getVariables())
        .build();
  }

  private void extractConnectorIdentifier(List<String> connectorIdentifierList, StepElementConfig stepElementConfig) {
    if (stepElementConfig.getStepSpecType() instanceof CIStepInfo) {
      CIStepInfo ciStepInfo = (CIStepInfo) stepElementConfig.getStepSpecType();
      if (ciStepInfo.getNonYamlInfo().getStepInfoType() == CIStepInfoType.RUN) {
        RunStepInfo runStepInfo = (RunStepInfo) ciStepInfo;
        if (runStepInfo.getImage() != null) {
          if (runStepInfo.getConnectorRef() == null) {
            throw new CIStageExecutionException("connector ref can't be empty if image is provided");
          } else {
            String identifier = resolveStringParameter(
                "connectorRef", "Run", ciStepInfo.getIdentifier(), runStepInfo.getConnectorRef(), false);
            if (!StringUtils.isEmpty(identifier)) {
              connectorIdentifierList.add(identifier);
            }
          }
        }
      }
    }
  }
}
