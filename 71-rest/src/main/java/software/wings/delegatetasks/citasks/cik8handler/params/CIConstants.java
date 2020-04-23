package software.wings.delegatetasks.citasks.cik8handler.params;

public final class CIConstants {
  private CIConstants() {
    // do nothing
  }
  public static final String VOLUME_MOUNT_PREFIX = "/harness-";
  public static final String STEP_EXEC_VOL_NAME = "volume-step-exec";
  public static final String MEMORY = "memory";
  public static final String MEMORY_FORMAT = "Mi";
  public static final String CPU = "cpu";
  public static final String CPU_FORMAT = "m";
  public static final String RESTART_POLICY = "Never";
  public static final long POD_MAX_TTL_SECS = 86400L; // 1 day
}