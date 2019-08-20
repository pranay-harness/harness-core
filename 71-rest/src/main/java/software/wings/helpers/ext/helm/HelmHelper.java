package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.exception.WingsException.USER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DOCKER_IMAGE_NAME_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DOCKER_IMAGE_TAG_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER;

import com.google.inject.Singleton;

import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.expression.ExpressionEvaluator;
import org.apache.commons.io.LineIterator;

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
}
