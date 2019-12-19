package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static io.harness.govern.Switch.unhandled;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DOCKER_IMAGE_NAME_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DOCKER_IMAGE_TAG_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import org.apache.commons.io.LineIterator;
import software.wings.beans.HelmExecutionSummary;
import software.wings.beans.appmanifest.StoreType;
import software.wings.beans.container.HelmChartSpecification;
import software.wings.beans.settings.helm.AmazonS3HelmRepoConfig;
import software.wings.beans.settings.helm.GCSHelmRepoConfig;
import software.wings.beans.settings.helm.HelmRepoConfig;
import software.wings.beans.settings.helm.HttpHelmRepoConfig;
import software.wings.helpers.ext.helm.request.HelmChartConfigParams;
import software.wings.helpers.ext.helm.response.HelmChartInfo;
import software.wings.helpers.ext.k8s.request.K8sDelegateManifestConfig;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

@Singleton
public class HelmHelper {
  public void validateHelmValueYamlFile(String helmValueYamlFile) {
    if (isEmpty(helmValueYamlFile)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER).addParam("args", "Helm value yaml file is empty");
    }

    try (LineIterator lineIterator = new LineIterator(new StringReader(helmValueYamlFile))) {
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains(HELM_NAMESPACE_PLACEHOLDER)) {
          return;
        }
      }
    } catch (IOException exception) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
          .addParam("args", "Helm value yaml file must contain " + HELM_NAMESPACE_PLACEHOLDER + " placeholder");
    }

    throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
        .addParam("args", "Helm value yaml file must contain " + HELM_NAMESPACE_PLACEHOLDER + " placeholder");
  }

  private static boolean checkStringPresentInHelmValueYaml(String helmValueYamlFile, String valueToFind) {
    if (isBlank(helmValueYamlFile)) {
      return false;
    }

    try (LineIterator lineIterator = new LineIterator(new StringReader(helmValueYamlFile))) {
      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains(valueToFind)) {
          return true;
        }
      }
    } catch (IOException exception) {
      return false;
    }

    return false;
  }

  public static boolean isArtifactReferencedInValuesYaml(String helmValueYamlFile) {
    Set<String> serviceArtifactVariableNames = new HashSet<>();
    updateArtifactVariableNamesReferencedInValuesYaml(helmValueYamlFile, serviceArtifactVariableNames);
    return isNotEmpty(serviceArtifactVariableNames);
  }

  public static void updateArtifactVariableNamesReferencedInValuesYaml(
      String helmValueYamlFile, Set<String> serviceArtifactVariableNames) {
    ExpressionEvaluator.updateServiceArtifactVariableNames(helmValueYamlFile, serviceArtifactVariableNames);
    if (!serviceArtifactVariableNames.contains(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME)
        && (checkStringPresentInHelmValueYaml(helmValueYamlFile, HELM_DOCKER_IMAGE_NAME_PLACEHOLDER)
               || checkStringPresentInHelmValueYaml(helmValueYamlFile, HELM_DOCKER_IMAGE_TAG_PLACEHOLDER))) {
      serviceArtifactVariableNames.add(ExpressionEvaluator.DEFAULT_ARTIFACT_VARIABLE_NAME);
    }
  }

  public HelmExecutionSummary prepareHelmExecutionSummary(
      String releaseName, HelmChartSpecification helmChartSpec, K8sDelegateManifestConfig delegateManifestConfig) {
    HelmChartInfo helmChartInfo = HelmChartInfo.builder().build();

    if (helmChartSpec != null) {
      if (isNotBlank(helmChartSpec.getChartName())) {
        helmChartInfo.setName(helmChartSpec.getChartName());
      }

      if (isNotBlank(helmChartSpec.getChartUrl())) {
        helmChartInfo.setRepoUrl(helmChartSpec.getChartUrl());
      }

      if (isNotBlank(helmChartSpec.getChartVersion())) {
        helmChartInfo.setVersion(helmChartSpec.getChartVersion());
      }
    } else if (delegateManifestConfig != null) {
      StoreType storeType = delegateManifestConfig.getManifestStoreTypes();

      switch (storeType) {
        case HelmSourceRepo:
          helmChartInfo.setRepoUrl(delegateManifestConfig.getGitConfig().getRepoUrl());
          break;

        case HelmChartRepo:
          HelmChartConfigParams helmChartConfigParams = delegateManifestConfig.getHelmChartConfigParams();
          if (isNotBlank(helmChartConfigParams.getChartName())) {
            helmChartInfo.setName(helmChartConfigParams.getChartName());
          }

          if (isNotBlank(helmChartConfigParams.getChartVersion())) {
            helmChartInfo.setVersion(helmChartConfigParams.getChartVersion());
          }

          helmChartInfo.setRepoUrl(getRepoUrlForHelmRepoConfig(helmChartConfigParams));
          break;

        default:
          unhandled(storeType);
      }
    }

    return HelmExecutionSummary.builder().releaseName(releaseName).helmChartInfo(helmChartInfo).build();
  }

  public String getRepoUrlForHelmRepoConfig(HelmChartConfigParams helmChartConfigParams) {
    HelmRepoConfig helmRepoConfig = helmChartConfigParams.getHelmRepoConfig();
    if (helmRepoConfig == null) {
      if (isNotBlank(helmChartConfigParams.getChartUrl())) {
        return helmChartConfigParams.getChartUrl();
      } else {
        String chartName = helmChartConfigParams.getChartName();
        return chartName.substring(0, chartName.indexOf('/'));
      }
    } else if (helmRepoConfig instanceof HttpHelmRepoConfig) {
      return ((HttpHelmRepoConfig) helmRepoConfig).getChartRepoUrl();
    } else if (helmRepoConfig instanceof AmazonS3HelmRepoConfig) {
      AmazonS3HelmRepoConfig amazonS3HelmRepoConfig = (AmazonS3HelmRepoConfig) helmRepoConfig;
      return new StringBuilder("s3://")
          .append(amazonS3HelmRepoConfig.getBucketName())
          .append("/")
          .append(helmChartConfigParams.getBasePath())
          .toString();
    } else if (helmRepoConfig instanceof GCSHelmRepoConfig) {
      GCSHelmRepoConfig gcsHelmRepoConfig = (GCSHelmRepoConfig) helmRepoConfig;

      return new StringBuilder("gs://")
          .append(gcsHelmRepoConfig.getBucketName())
          .append("/")
          .append(helmChartConfigParams.getBasePath())
          .toString();
    } else {
      return null;
    }
  }
}
