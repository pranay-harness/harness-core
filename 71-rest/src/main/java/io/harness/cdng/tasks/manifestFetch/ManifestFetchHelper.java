package io.harness.cdng.tasks.manifestFetch;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.ManifestType;
import io.harness.cdng.manifest.ValuesPathProvider;
import io.harness.cdng.manifest.yaml.GitStore;
import io.harness.cdng.manifest.yaml.ManifestAttributes;
import io.harness.cdng.tasks.manifestFetch.beans.GitFetchFilesConfig;
import io.harness.cdng.tasks.manifestFetch.step.ManifestFetchParameters;
import io.harness.security.encryption.EncryptedDataDetail;
import software.wings.beans.GitConfig;
import software.wings.beans.SettingAttribute;
import software.wings.service.intfc.SettingsService;
import software.wings.service.intfc.security.SecretManager;

import java.util.ArrayList;
import java.util.List;

@Singleton
public class ManifestFetchHelper {
  @Inject SettingsService settingsService;
  @Inject SecretManager secretManager;

  public List<GitFetchFilesConfig> generateFetchFilesConfigForManifests(
      ManifestFetchParameters manifestFetchStepParameters) {
    List<GitFetchFilesConfig> gitFetchFilesConfigs = new ArrayList<>();
    List<ManifestAttributes> manifestAttributesSpec = manifestFetchStepParameters.getServiceSpecManifestAttributes();

    if (isNotEmpty(manifestAttributesSpec)) {
      manifestAttributesSpec.forEach(manifestAttribute -> {
        List<String> paths;
        GitStore gitStore = (GitStore) manifestAttribute.getStoreConfig();
        if (ManifestStoreType.GIT.equals(manifestAttribute.getStoreConfig().getKind())) {
          paths = fetchPathsToFetch(manifestAttribute, gitStore);

          if (isNotEmpty(paths)) {
            String connectorId = gitStore.getConnectorId();
            SettingAttribute settingAttribute = settingsService.get(connectorId);

            if (settingAttribute != null) {
              GitConfig gitConfig = (GitConfig) settingAttribute.getValue();
              List<EncryptedDataDetail> encryptionDetails = secretManager.getEncryptionDetails(gitConfig, "1234", null);
              gitFetchFilesConfigs.add(
                  GitFetchFilesConfig.builder()
                      .paths(paths)
                      .gitConfig(gitConfig)
                      .gitStore(gitStore)
                      .encryptedDataDetails(encryptionDetails)
                      .identifier(manifestAttribute.getIdentifier())
                      .succeedIfFileNotFound(ManifestType.K8Manifest.equals(manifestAttribute.getKind()))
                      .build());
            }
          }
        }
      });
    }

    return gitFetchFilesConfigs;
  }

  private List<String> fetchPathsToFetch(ManifestAttributes manifestAttribute, GitStore gitStore) {
    List<String> paths;
    if (manifestAttribute instanceof ValuesPathProvider) {
      paths = ((ValuesPathProvider) manifestAttribute).getValuesPathsToFetch();
    } else {
      paths = gitStore.getPaths();
    }
    return paths;
  }
}
