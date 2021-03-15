package io.harness.plancreator.steps.approval;

import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.Builder;
import lombok.Value;

/**
 * Approvers for Approval Step.
 */
@Value
@Builder
public class Approvers {
  ParameterField<List<String>> userGroups;

  ParameterField<List<String>> users;

  // Minimum Number of approvals required.
  ParameterField<Integer> minimumCount;

  ParameterField<Boolean> disallowPipelineExecutor;
}
