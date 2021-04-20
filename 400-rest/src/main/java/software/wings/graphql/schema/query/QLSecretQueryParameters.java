package software.wings.graphql.schema.query;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import software.wings.graphql.schema.type.secrets.QLSecretType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSecretQueryParametersKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLSecretQueryParameters {
  String name;
  String secretId;
  QLSecretType secretType;
}
