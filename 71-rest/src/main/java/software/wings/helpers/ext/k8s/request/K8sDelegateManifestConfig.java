package software.wings.helpers.ext.k8s.request;

import io.harness.security.encryption.EncryptedDataDetail;
import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.kustomize.KustomizeConfig;

import java.util.List;

@Data
@Builder
public class K8sDelegateManifestConfig {
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
}
