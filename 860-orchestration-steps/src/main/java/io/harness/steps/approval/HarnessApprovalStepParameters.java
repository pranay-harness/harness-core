package io.harness.steps.approval;

import io.harness.plancreator.steps.approval.HarnessApprovalBaseStepInfo;
import io.harness.pms.sdk.core.steps.io.StepParameters;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.data.annotation.TypeAlias;

@Data
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
@TypeAlias("harnessApprovalStepParameters")
public class HarnessApprovalStepParameters extends HarnessApprovalBaseStepInfo implements StepParameters {}
