/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package software.wings.signup;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;

import software.wings.app.BugsnagErrorReporterConfiguration;
import software.wings.beans.BugsnagTab;
import software.wings.beans.ErrorData;
import software.wings.service.intfc.ErrorReporter;

import com.bugsnag.Bugsnag;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@OwnedBy(PL)
@Slf4j
public class BugsnagErrorReporter implements ErrorReporter {
  @Inject private BugsnagErrorReporterConfiguration bugsnagErrorReporterConfiguration;

  public void report(ErrorData errorData) {
    String bugsnagApiKey = bugsnagErrorReporterConfiguration.getBugsnagApiKey();
    if (bugsnagErrorReporterConfiguration.isErrorReportingEnabled() && StringUtils.isNotEmpty(bugsnagApiKey)) {
      try (Bugsnag bugsnag = new Bugsnag(bugsnagApiKey)) {
        log.info("Sending alert to bugsnag");
        bugsnag.addCallback(report -> {
          for (BugsnagTab tab : errorData.getTabs()) {
            report.addToTab(tab.getTabName(), tab.getKey(), tab.getValue());
          }
          report.setUserEmail(errorData.getEmail());
        });
        bugsnag.notify(errorData.getException());
      } catch (Exception ex) {
        log.info("Exception occurred while reporting error to bugsnag", ex);
      }
    }
  }
}
