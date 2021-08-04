package software.wings.graphql.schema.type.secrets;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.utils.RequestField;

import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.Scope;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldNameConstants;

@OwnedBy(PL)
@Getter
@Setter
@Builder
@FieldNameConstants(innerTypeName = "QLEncryptedFileUpdateKeys")
@Scope(PermissionAttribute.ResourceType.SETTING)
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class QLEncryptedFileUpdate {
  RequestField<String> name;
  RequestField<byte[]> fileContent;
  RequestField<Boolean> scopedToAccount;
  RequestField<QLUsageScope> usageScope;
  RequestField<Boolean> inheritScopesFromSM;
}
