package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;
import software.wings.graphql.schema.type.QLObject;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLKeyTabFileInputKeys")
public class QLKeyTabFile implements QLObject {
  String filePath;
}
