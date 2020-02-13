package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKerberosAuthenticationInputKeys")
public class QLKerberosAuthenticationInput implements QLObject {
  String principal;
  String realm;
  Integer port;
  QLTGTGenerationMethod tgtGenerationMethod;
}
