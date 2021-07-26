package io.harness.cdng.manifest.mappers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.common.ParameterFieldHelper.getParameterFieldValue;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.manifest.ManifestType.HelmChart;
import static io.harness.manifest.ManifestType.Kustomize;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.manifest.ManifestStoreType;
import io.harness.cdng.manifest.yaml.GcsStoreConfig;
import io.harness.cdng.manifest.yaml.GitStoreConfig;
import io.harness.cdng.manifest.yaml.S3StoreConfig;
import io.harness.delegate.beans.storeconfig.FetchType;
import io.harness.exception.InvalidArgumentsException;
import io.harness.manifest.HelmChartManifestOutcome;
import io.harness.manifest.ManifestOutcome;
import io.harness.manifest.ManifestType;
import io.harness.manifest.storeConfig.StoreConfig;
import io.harness.pms.yaml.ParameterField;

import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.tuple.Pair;

@OwnedBy(CDP)
@UtilityClass
public class ManifestOutcomeValidator {
  public void validate(ManifestOutcome manifestOutcome, boolean allowExpression) {
    if (manifestOutcome.getStore() != null) {
      validateStore(
          manifestOutcome.getStore(), manifestOutcome.getType(), manifestOutcome.getIdentifier(), allowExpression);
    }

    if (ManifestType.HelmChart.equals(manifestOutcome.getType())) {
      validateHelmChartManifest((HelmChartManifestOutcome) manifestOutcome, allowExpression);
    }
  }

  public void validateStore(StoreConfig store, String manifestKind, String manifestId, boolean allowExpression) {
    if (ManifestStoreType.isInGitSubset(store.getKind())) {
      validateGitStore((GitStoreConfig) store, manifestKind, manifestId, allowExpression);
    } else if (ManifestStoreType.S3.equals(store.getKind())) {
      validateS3Store((S3StoreConfig) store, manifestId, allowExpression);
    } else if (ManifestStoreType.GCS.equals(store.getKind())) {
      validateGcsStore((GcsStoreConfig) store, manifestId, allowExpression);
    }
  }

  private void validateHelmChartManifest(HelmChartManifestOutcome helmChartManifest, boolean allowExpression) {
    String manifestStoreKind = helmChartManifest.getStore().getKind();
    if (!ManifestStoreType.isInGitSubset(manifestStoreKind)) {
      if (!hasValue(helmChartManifest.getChartName(), allowExpression)) {
        throw new InvalidArgumentsException(
            Pair.of("chartName", format("required for %s store type", manifestStoreKind)));
      }
    } else {
      if (hasValue(helmChartManifest.getChartName(), allowExpression)) {
        throw new InvalidArgumentsException(
            Pair.of("chartName", format("not allowed for %s store type", manifestStoreKind)));
      }
    }

    if (hasValue(helmChartManifest.getChartVersion(), allowExpression)) {
      if (ManifestStoreType.isInGitSubset(manifestStoreKind)) {
        throw new InvalidArgumentsException(
            Pair.of("chartVersion", format("not allowed for %s store", manifestStoreKind)));
      }
    }
  }

  private void validateGitStore(GitStoreConfig store, String manifestKind, String manifestId, boolean allowExpression) {
    if (!hasValue(store.getConnectorRef(), allowExpression)) {
      throw new InvalidArgumentsException(
          format("Missing or empty connectorRef in %s store spec for manifest with identifier: %s", store.getKind(),
              manifestId));
    }

    switch (manifestKind) {
      case HelmChart:
      case Kustomize:
        if (!hasValue(store.getFolderPath(), allowExpression)) {
          throw new InvalidArgumentsException(Pair.of("folderPath",
              format("is required for store type '%s' and manifest type '%s' in manifest with identifier: %s",
                  store.getKind(), manifestKind, manifestId)));
        }
        break;

      default:
        if (!hasValue(store.getPaths())) {
          throw new InvalidArgumentsException(Pair.of("paths",
              format("is required for store type '%s' and manifest type '%s' in manifest with identifier: %s",
                  store.getKind(), manifestKind, manifestId)));
        }
    }

    if (FetchType.BRANCH == store.getGitFetchType()) {
      if (hasValue(store.getCommitId(), allowExpression)) {
        throw new InvalidArgumentsException(Pair.of("commitId", "Not allowed for gitFetchType: Branch"));
      }

      if (!hasValue(store.getBranch(), allowExpression)) {
        throw new InvalidArgumentsException(Pair.of("branch", "Cannot be empty or null for gitFetchType: Branch"));
      }
    }

    if (FetchType.COMMIT == store.getGitFetchType()) {
      if (hasValue(store.getBranch(), allowExpression)) {
        throw new InvalidArgumentsException(Pair.of("branch", "Not allowed for gitFetchType: Commit"));
      }

      if (!hasValue(store.getCommitId(), allowExpression)) {
        throw new InvalidArgumentsException(Pair.of("commitId", "Cannot be empty or null for gitFetchType: Commit"));
      }
    }
  }

  private void validateS3Store(S3StoreConfig store, String manifestId, boolean allowExpression) {
    if (!hasValue(store.getConnectorRef(), allowExpression)) {
      throw new InvalidArgumentsException(
          format("Missing or empty connectorRef in S3 store spec for manifest with identifier: %s", manifestId));
    }

    if (!hasValue(store.getRegion(), allowExpression)) {
      throw new InvalidArgumentsException(Pair.of("region", "Cannot be empty or null for S3 store"));
    }

    if (!hasValue(store.getBucketName(), allowExpression)) {
      throw new InvalidArgumentsException(Pair.of("bucketName", "Cannot be empty or null for S3 store"));
    }
  }

  private void validateGcsStore(GcsStoreConfig store, String manifestId, boolean allowExpression) {
    if (!hasValue(store.getConnectorRef(), allowExpression)) {
      throw new InvalidArgumentsException(
          format("Missing or empty connectorRef in Gcs store spec for manifest with identifier: %s", manifestId));
    }

    if (!hasValue(store.getBucketName(), allowExpression)) {
      throw new InvalidArgumentsException(Pair.of("bucketName", "Cannot be empty or null for Gcs store"));
    }
  }

  private boolean hasValue(ParameterField<String> parameterField, boolean allowExpression) {
    if (ParameterField.isNull(parameterField)) {
      return false;
    }

    if (allowExpression && parameterField.isExpression()) {
      return true;
    }

    return isNotEmpty(getParameterFieldValue(parameterField));
  }

  private boolean hasValue(ParameterField<List<String>> parameterField) {
    return !ParameterField.isNull(parameterField) || isNotEmpty(getParameterFieldValue(parameterField));
  }
}
