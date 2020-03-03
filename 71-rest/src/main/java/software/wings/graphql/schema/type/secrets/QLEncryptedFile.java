package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedFileKeys")
public class QLEncryptedFile implements QLSecret {
  private String secretManagerId;
  private String name;
  private QLSecretType secretType;
  private String id;
  private QLUsageScope usageScope;
}
