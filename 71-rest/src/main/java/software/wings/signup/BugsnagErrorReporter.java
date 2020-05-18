package software.wings.signup;

import com.google.inject.Inject;

import com.bugsnag.Bugsnag;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import software.wings.app.BugsnagErrorReporterConfiguration;
import software.wings.beans.ErrorData;
import software.wings.service.intfc.ErrorReporter;

@Slf4j
public class BugsnagErrorReporter implements ErrorReporter {
  @Inject private BugsnagErrorReporterConfiguration bugsnagErrorReporterConfiguration;

  private static final String CLUSTER_TYPE = "clusterType";
  private static final String FREEMIUM = "freemium";
  private static final String ONBOARDING = "Onboarding";

  public void report(ErrorData errorData) {
    String bugsnagApiKey = bugsnagErrorReporterConfiguration.getBugsnagApiKey();
    if (bugsnagErrorReporterConfiguration.isErrorReportingEnabled() && StringUtils.isNotEmpty(bugsnagApiKey)) {
      try (Bugsnag bugsnag = new Bugsnag(bugsnagApiKey)) {
        logger.info("Sending alert to bugsnag");
        bugsnag.addCallback(report -> {
          report.addToTab(CLUSTER_TYPE, FREEMIUM, ONBOARDING);
          report.setUserEmail(errorData.getEmail());
        });
        bugsnag.notify(errorData.getException());
      } catch (Exception ex) {
        logger.info("Exception occurred while reporting error to bugsnag", ex);
      }
    }
  }
}
