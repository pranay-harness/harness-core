package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLCreateEncryptedTextInputKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEncryptedTextInput {
  private String secretManagerId;
  private String name;
  private String value;
  private String secretReference;
  private QLUsageScope usageScope;
  private boolean scopedToAccount;
  private boolean inheritScopesFromSM;
}
