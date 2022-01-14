package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedTextKeys")
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEncryptedText implements QLSecret {
  private String secretManagerId;
  private String name;
  private QLSecretType secretType;
  private String id;
  private QLUsageScope usageScope;
  private boolean scopedToAccount;
  private boolean inheritScopesFromSM;
  private String secretReference;
}
