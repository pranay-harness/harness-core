/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;

import software.wings.beans.container.PcfServiceSpecification;

import com.google.inject.Singleton;
import java.io.BufferedReader;
import java.io.StringReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
@OwnedBy(CDC)
@TargetModule(HarnessModule._870_CG_ORCHESTRATION)
public class ServiceHelper {
  public static final String APP_NAME_PATTERN = "^\\s*[-]\\s+name\\s*:";
  public static final String INSTANCE_PATTERN = "^\\s*instances\\s*:";
  public static final String PATH_PATTERN = "^\\s*path\\s*:";

  public void addPlaceholderTexts(PcfServiceSpecification pcfServiceSpecification) {
    String manifestYaml = pcfServiceSpecification.getManifestYaml();
    StringBuilder sb = new StringBuilder(128);

    BufferedReader bufReader = new BufferedReader(new StringReader(manifestYaml));
    String line;
    try {
      while ((line = bufReader.readLine()) != null) {
        boolean matchFound = checkAndProcessApplicationName(line, sb);

        if (!matchFound) {
          matchFound = checkAndProcessInstance(line, sb);
        }

        if (!matchFound) {
          matchFound = checkAndProcessPath(line, sb);
        }

        if (!matchFound) {
          sb.append(line).append('\n');
        }
      }
    } catch (Exception e) {
      log.error("", e);
    }
    pcfServiceSpecification.setManifestYaml(sb.toString());
  }

  private Matcher checkAndGetMatcher(String line, String patternString) {
    Pattern pattern = Pattern.compile(patternString);
    Matcher matcher = pattern.matcher(line);
    if (matcher.find()) {
      return matcher;
    }

    return null;
  }

  private boolean checkAndProcessApplicationName(String line, StringBuilder sb) {
    Matcher matcher = checkAndGetMatcher(line, APP_NAME_PATTERN);
    if (matcher != null) {
      sb.append(matcher.group(0)).append(" ${APPLICATION_NAME}\n");
      return true;
    }

    return false;
  }

  private boolean checkAndProcessInstance(String line, StringBuilder sb) {
    Matcher matcher = checkAndGetMatcher(line, INSTANCE_PATTERN);
    if (matcher != null) {
      sb.append(matcher.group(0)).append(" ${INSTANCE_COUNT}\n");
      return true;
    }

    return false;
  }

  private boolean checkAndProcessPath(String line, StringBuilder sb) {
    Matcher matcher = checkAndGetMatcher(line, PATH_PATTERN);
    if (matcher != null) {
      sb.append(matcher.group(0)).append(" ${FILE_LOCATION}\n");
      return true;
    }

    return false;
  }
}
