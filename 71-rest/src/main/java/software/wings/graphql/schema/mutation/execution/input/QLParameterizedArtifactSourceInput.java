package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import java.util.List;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLParameterizedArtifactSourceInputKeys")
@Scope(PermissionAttribute.ResourceType.DEPLOYMENT)
public class QLParameterizedArtifactSourceInput {
  String artifactSourceName;
  String buildNumber;
  List<QLParameterValueInput> parameterValueInputs;
}
