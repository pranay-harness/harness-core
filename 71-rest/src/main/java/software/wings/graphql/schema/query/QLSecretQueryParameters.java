package software.wings.graphql.schema.query;

import software.wings.graphql.schema.type.secrets.QLSecretType;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSecretQueryParametersKeys")
public class QLSecretQueryParameters {
  String name;
  String secretId;
  QLSecretType secretType;
}
