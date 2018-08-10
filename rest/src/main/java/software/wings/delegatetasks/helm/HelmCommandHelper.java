package software.wings.delegatetasks.helm;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.inject.Singleton;

import com.esotericsoftware.yamlbeans.YamlReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ErrorCode;
import software.wings.exception.WingsException;
import software.wings.helpers.ext.helm.request.HelmCommandRequest;

import java.util.Map;
import java.util.Optional;

@Singleton
public class HelmCommandHelper {
  private static final Logger logger = LoggerFactory.getLogger(HelmCommandHelper.class);
  public static final String URL = "url";
  public static final String NAME = "name";
  public static final String VERSION = "version";
  public static final String HARNESS = "harness";
  public static final String HELM = "helm";
  public static final String CHART = "chart";

  public String getDeploymentMessage(HelmCommandRequest helmCommandRequest) {
    switch (helmCommandRequest.getHelmCommandType()) {
      case INSTALL:
        return "Installing";
      case ROLLBACK:
        return "Rolling back";
      case RELEASE_HISTORY:
        return "Getting release history";
      default:
        return "Unsupported operation";
    }
  }

  public Optional<HarnessHelmDeployConfig> generateHelmDeployChartSpecFromYaml(String yamlString) {
    YamlReader reader = new YamlReader(yamlString);
    try {
      // A YAML can contain more than one YAML document.
      // Call to YamlReader.read() deserializes the next document into an object.
      // YAML documents are delimited by "---"
      while (true) {
        Map map = (Map) reader.read();
        if (map == null) {
          break;
        }

        /*
         * harness:
         *   helm:
         *      chart:
         *         url: google.com
         *         name: abc
         *         version:1.0
         *      timeout:10  // this is a pseudo field
         *      releasePrefixName: aaaa // this is a pseudo field
         * */
        if (map.containsKey(HARNESS)) {
          Map harnessDataMap = (Map) map.get(HARNESS);

          if (isNotEmpty(harnessDataMap) && harnessDataMap.containsKey(HELM)) {
            Map harnessHelmDataMap = (Map) harnessDataMap.get(HELM);
            if (isNotEmpty(harnessHelmDataMap) && harnessHelmDataMap.containsKey(CHART)) {
              Map harnessHelmChartDataMap = (Map) harnessHelmDataMap.get(CHART);
              HelmDeployChartSpec helmDeployChartSpec = HelmDeployChartSpec.builder()
                                                            .url((String) harnessHelmChartDataMap.get(URL))
                                                            .name((String) harnessHelmChartDataMap.get(NAME))
                                                            .version((String) harnessHelmChartDataMap.get(VERSION))
                                                            .build();

              // Add any other fields under helm if added later
              return Optional.of(HarnessHelmDeployConfig.builder().helmDeployChartSpec(helmDeployChartSpec).build());
            }
          }
        }
      }
    } catch (Exception e) {
      logger.error("Failed while parsing yamlString:" + yamlString);
      throw new WingsException(
          ErrorCode.GENERAL_ERROR, "Invalid Yaml, Failed while parsing yamlString", WingsException.SRE);
    }

    return Optional.empty();
  }
}
