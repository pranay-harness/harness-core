package io.harness.delegate.task.citasks.awsvm.helper;

public class CIAwsVmConstants {
  public static final String RUNNER_URL = "http://127.0.0.1:3000/";
  public static final String RUNNER_SETUP_STAGE_URL = RUNNER_URL + "setup";
  public static final String RUNNER_EXECUTE_STEP_URL = RUNNER_URL + "step";
  public static final String RUNNER_CLEANUP_STAGE_URL = RUNNER_URL + "destroy";
  public static final int RUNNER_CONNECT_TIMEOUT_SECS = 1;
  public static final int RUNNER_WRITE_TIMEOUT_SECS = 1;
}
