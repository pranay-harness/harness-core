package software.wings.graphql.schema.type.secrets;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
public class QLEncryptedTextInput {
  private String secretManagerId;
  private String name;
  private String value;
  private String clientMutationId;
  private QLUsageScope usageScope;
}
