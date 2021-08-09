package software.wings.app;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BugsnagErrorReporterConfiguration {
  private boolean errorReportingEnabled;
  private String bugsnagApiKey;
}
