package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLTGTGenerationMethodKeys")
public class QLTGTGenerationMethod implements QLObject {
  QLTGTGenerationUsing tgtGenerationUsing;
  QLKeyTabFile keyTabFile;
  QLKerberosPassword kerberosPassword;
}
