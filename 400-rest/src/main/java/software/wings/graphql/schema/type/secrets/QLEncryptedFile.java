package software.wings.graphql.schema.type.secrets;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;

import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedFileKeys")
@TargetModule(Module._380_CG_GRAPHQL)
public class QLEncryptedFile implements QLSecret {
  private String secretManagerId;
  private String name;
  private QLSecretType secretType;
  private String id;
  private QLUsageScope usageScope;
  private boolean scopedToAccount;
  private boolean inheritScopesFromSM;
}
