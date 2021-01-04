package software.wings.helpers.ext.k8s.request;

import static io.harness.expression.Expression.ALLOW_SECRETS;

import io.harness.expression.Expression;
import io.harness.expression.ExpressionReflectionUtils.NestedAnnotationResolver;
import io.harness.security.encryption.EncryptedDataDetail;

import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.HelmCommandFlag;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class K8sDelegateManifestConfig implements NestedAnnotationResolver {
  StoreType manifestStoreTypes;
  List<ManifestFile> manifestFiles;
  List<EncryptedDataDetail> encryptedDataDetails;

  // Applies to GitFileConfig
  private GitFileConfig gitFileConfig;
  private GitConfig gitConfig;

  // Applies only to HelmRepoConfig
  private HelmChartConfigParams helmChartConfigParams;

  // Applies only to Kustomize
  private KustomizeConfig kustomizeConfig;

  @Expression(ALLOW_SECRETS) private HelmCommandFlag helmCommandFlag;
}
