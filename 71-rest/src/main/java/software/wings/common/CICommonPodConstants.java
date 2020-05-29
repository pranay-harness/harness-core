package software.wings.common;

public class CICommonPodConstants {
  public static final String POD_NAME = "ci-build-pod";
  public static final String STEP_EXEC_WORKING_DIR = "workspace";
  public static final String CONTAINER_NAME = "build-setup";
  public static final String STEP_EXEC = "step-exec";
  public static final String MOUNT_PATH = "/step-exec";
  public static final String REL_STDOUT_FILE_PATH = "/stdout";
  public static final String REL_STDERR_FILE_PATH = "/stderr";
  public static final String CLUSTER_EXPRESSION = "${input.cluster}";
  public static final String NAMESPACE_EXPRESSION = "${input.namespace}";
  public static final String POD_NAME_EXPRESSION = "${input.podName}";
}
