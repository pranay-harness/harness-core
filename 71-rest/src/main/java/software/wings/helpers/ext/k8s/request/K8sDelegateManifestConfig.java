package software.wings.helpers.ext.k8s.request;

import lombok.Builder;
import lombok.Data;
import software.wings.beans.GitConfig;
import software.wings.beans.GitFileConfig;
import software.wings.beans.appmanifest.ManifestFile;
import software.wings.beans.appmanifest.StoreType;
import software.wings.security.encryption.EncryptedDataDetail;

import java.util.List;

@Data
@Builder
public class K8sDelegateManifestConfig {
  StoreType manifestStoreTypes;
  List<ManifestFile> manifestFiles;
  GitFileConfig gitFileConfig;
  GitConfig gitConfig;
  List<EncryptedDataDetail> encryptedDataDetails;
}
