package software.wings.delegatetasks.citasks.cik8handler.params;

public final class CIConstants {
  private CIConstants() {
    // do nothing
  }

  public static final String MEMORY = "memory";
  public static final String MEMORY_FORMAT = "Mi";
  public static final String CPU = "cpu";
  public static final String CPU_FORMAT = "m";
  public static final String RESTART_POLICY = "Never";

  public static final long POD_MAX_TTL_SECS = 86400L; // 1 day
  public static final String POD_PENDING_PHASE = "Pending";
  public static final String POD_RUNNING_PHASE = "Running";
  public static final int POD_MAX_WAIT_UNTIL_READY_SECS = 300; // 5 minutes
  public static final int POD_WAIT_UNTIL_READY_SLEEP_SECS = 2; // Time to sleep in between pod status checks
}