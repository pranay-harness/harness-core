package software.wings.helpers.ext.helm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.wings.exception.WingsException.USER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_DOCKER_IMAGE_NAME_PLACEHOLDER;
import static software.wings.helpers.ext.helm.HelmConstants.HELM_NAMESPACE_PLACEHOLDER;

import com.google.inject.Singleton;

import org.apache.commons.io.LineIterator;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;

import java.io.StringReader;

@Singleton
public class HelmHelper {
  public void validateHelmValueYamlFile(String helmValueYamlFile) {
    if (isNotEmpty(helmValueYamlFile)) {
      boolean foundNamespaceHolder = false;
      LineIterator lineIterator = new LineIterator(new StringReader(helmValueYamlFile));

      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains(HELM_NAMESPACE_PLACEHOLDER)) {
          foundNamespaceHolder = true;
        }
      }

      if (!foundNamespaceHolder) {
        throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER)
            .addParam("args", "Helm value yaml file must contain " + HELM_NAMESPACE_PLACEHOLDER + " placeholder");
      }
    } else {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT, USER).addParam("args", "Helm value yaml file is empty");
    }
  }

  private static boolean checkStringPresentInHelmValueYaml(String helmValueYamlFile, String valueToFind) {
    boolean found = false;

    if (isNotBlank(helmValueYamlFile)) {
      LineIterator lineIterator = new LineIterator(new StringReader(helmValueYamlFile));

      while (lineIterator.hasNext()) {
        String line = lineIterator.nextLine();
        if (isBlank(line) || line.trim().charAt(0) == '#') {
          continue;
        }
        if (line.contains(valueToFind)) {
          found = true;
          break;
        }
      }
    }

    return found;
  }

  public static boolean checkDockerImageNamePresentInValuesYaml(String helmValueYamlFile) {
    return checkStringPresentInHelmValueYaml(helmValueYamlFile, HELM_DOCKER_IMAGE_NAME_PLACEHOLDER);
  }
}
