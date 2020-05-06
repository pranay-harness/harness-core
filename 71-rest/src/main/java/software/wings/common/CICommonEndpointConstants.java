package software.wings.common;

/**
 *  Common end points for CIServiceHelperResource
 */

public class CICommonEndpointConstants {
  public static final String CI_SETUP_ENDPOINT = "/setup"; // Endpoint for env setup, git clone and launching pod
  public static final String CI_COMMAND_EXECUTION_ENDPOINT = "/command-exec";
  public static final String CI_CLEANUP_ENDPOINT = "/cleanup";
}
