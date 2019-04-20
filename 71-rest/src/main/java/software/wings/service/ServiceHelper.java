package software.wings.service;

import com.google.inject.Singleton;

import lombok.extern.slf4j.Slf4j;
import software.wings.beans.container.PcfServiceSpecification;

import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Singleton
@Slf4j
public class ServiceHelper {
  public void addPlaceholderTexts(PcfServiceSpecification pcfServiceSpecification) {
    String manifestYaml = pcfServiceSpecification.getManifestYaml();
    StringBuilder sb = new StringBuilder(128);

    BufferedReader bufReader = new BufferedReader(new StringReader(manifestYaml));
    String line = null;

    Pattern patternAppName = Pattern.compile("^\\s*[-]\\s+name\\s*:");
    Pattern patternInstances = Pattern.compile("^\\s*instances\\s*:");
    Pattern patternPath = Pattern.compile("^\\s*path\\s*:");
    Pattern patternRoute = Pattern.compile("^\\s*[-]\\s+route\\s*:");
    boolean routeAdded = false;
    try {
      while ((line = bufReader.readLine()) != null) {
        Matcher matcher = patternAppName.matcher(line);
        if (matcher.find()) {
          sb.append(matcher.group(0)).append(" ${APPLICATION_NAME}\n");
          continue;
        }

        matcher = patternInstances.matcher(line);
        if (matcher.find()) {
          sb.append(matcher.group(0)).append(" ${INSTANCE_COUNT}\n");
          continue;
        }

        matcher = patternPath.matcher(line);
        if (matcher.find()) {
          sb.append(matcher.group(0)).append(" ${FILE_LOCATION}\n");
          continue;
        }

        matcher = patternRoute.matcher(line);
        if (matcher.find()) {
          if (!routeAdded) {
            routeAdded = true;
            sb.append(matcher.group(0)).append(" ${ROUTE_MAP}\n");
          }
          continue;
        }

        sb.append(line).append('\n');
      }
    } catch (Exception e) {
      logger.error("", e);
    }
    pcfServiceSpecification.setManifestYaml(sb.toString());
  }
}
