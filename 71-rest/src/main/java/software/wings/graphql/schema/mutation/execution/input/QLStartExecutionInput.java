package software.wings.graphql.schema.mutation.execution.input;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.mutation.QLMutationInput;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTriggerExecutionInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLStartExecutionInput implements QLMutationInput {
  String clientMutationId;
  String applicationId;
  String entityId;
  QLExecutionType executionType;
  List<QLVariableInput> variableInputs;
  List<QLServiceInput> serviceInputs;
  String notes;
  boolean excludeHostsWithSameArtifact;
  boolean targetToSpecificHosts;
  List<String> specificHosts;
}
