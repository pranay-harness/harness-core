package software.wings.graphql.schema.query;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.secrets.QLSecretType;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLSecretQueryParametersKeys")
public class QLSecretQueryParameters {
  String name;
  String secretId;
  QLSecretType secretType;
}
