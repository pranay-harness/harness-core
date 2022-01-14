package software.wings.beans;

import static io.harness.annotations.dev.HarnessModule._970_API_SERVICES_BEANS;
import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.security.encryption.EncryptedDataDetail;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@OwnedBy(CDP)
@TargetModule(_970_API_SERVICES_BEANS)
public class GitFetchFilesConfig implements NestedAnnotationResolver {
  @Expression(ALLOW_SECRETS) private GitFileConfig gitFileConfig;
  private GitConfig gitConfig;
  private List<EncryptedDataDetail> encryptedDataDetails;
}
