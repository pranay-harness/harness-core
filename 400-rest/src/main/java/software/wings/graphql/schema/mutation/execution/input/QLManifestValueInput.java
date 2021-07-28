package software.wings.graphql.schema.mutation.execution.input;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.OwnedBy;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@OwnedBy(CDC)
@Value
@Builder
@FieldNameConstants(innerTypeName = "QLManifestValueInputKeys")
public class QLManifestValueInput {
  QLManifestInputType valueType;
  String helmChartId;
  QLVersionNumberInput versionNumber;
}