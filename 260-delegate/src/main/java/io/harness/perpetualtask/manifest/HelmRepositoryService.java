package io.harness.perpetualtask.manifest;

import static io.harness.data.structure.UUIDGenerator.convertBase64UuidToCanonicalForm;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.manifests.request.ManifestCollectionParams;
import io.harness.exception.ManifestCollectionException;
import org.jetbrains.annotations.NotNull;
import software.wings.beans.appmanifest.HelmChart;
import software.wings.delegatetasks.helm.HelmTaskHelper;
import software.wings.helpers.ext.helm.request.HelmChartCollectionParams;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;

import java.util.List;

@Singleton
@OwnedBy(HarnessTeam.CDC)
public class HelmRepositoryService implements ManifestRepositoryService {
  private static final String MANIFEST_COLLECTION_DIR = "manifest-collection";
  private static final long TIMEOUT_IN_MILLIS = 90L * 1000;

  @Inject private HelmTaskHelper helmTaskHelper;

  @Override
  public List<HelmChart> collectManifests(ManifestCollectionParams params) {
    if (params instanceof HelmChartCollectionParams) {
      try {
        HelmChartCollectionParams helmChartCollectionParams = (HelmChartCollectionParams) params;
        HelmChartConfigParams helmChartConfigParams = helmChartCollectionParams.getHelmChartConfigParams();
        return helmTaskHelper.fetchChartVersions(
            helmChartCollectionParams, getDestinationDirectory(helmChartConfigParams), TIMEOUT_IN_MILLIS);
      } catch (Exception e) {
        throw new ManifestCollectionException("Exception while collecting manifests from repo: " + e.getMessage(), e);
      }
    } else {
      throw new ManifestCollectionException("Collection not yet implemented for given manifest type");
    }
  }

  @NotNull
  private String getDestinationDirectory(HelmChartConfigParams helmChartConfigParams) {
    return MANIFEST_COLLECTION_DIR + "-" + convertBase64UuidToCanonicalForm(helmChartConfigParams.getRepoName());
  }

  @Override
  public void cleanup(ManifestCollectionParams params) {
    if (params instanceof HelmChartCollectionParams) {
      try {
        HelmChartCollectionParams helmChartCollectionParams = (HelmChartCollectionParams) params;
        helmTaskHelper.cleanupAfterCollection(helmChartCollectionParams,
            getDestinationDirectory(helmChartCollectionParams.getHelmChartConfigParams()), TIMEOUT_IN_MILLIS);
      } catch (Exception e) {
        throw new ManifestCollectionException("Exception while cleaning up manifests: " + e.getMessage(), e);
      }
    } else {
      throw new ManifestCollectionException("Collection not yet implemented for given manifest type");
    }
  }
}
