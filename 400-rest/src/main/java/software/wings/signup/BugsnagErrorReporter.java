/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
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
