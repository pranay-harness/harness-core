package software.wings.helpers.ext.openshift;

import java.util.concurrent.TimeUnit;

public final class OpenShiftConstants {
  private OpenShiftConstants() {}

  public static final String TEMPLATE_FILE_PATH = "${TEMPLATE_FILE_PATH}";
  public static final String OC_BINARY_PATH = "${OC_BINARY_PATH}";
  public static final String PROCESS_COMMAND =
      OC_BINARY_PATH + " process -f " + TEMPLATE_FILE_PATH + " --local -o yaml";
  public static final long COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(1);
}
