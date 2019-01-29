package software.wings.helpers.ext.helm;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 3/23/18.
 */
public class HelmConstants {
  public static final String HELM_NAMESPACE_PLACEHOLDER_REGEX = "\\$\\{NAMESPACE}";
  public static final String HELM_NAMESPACE_PLACEHOLDER = "${NAMESPACE}";
  public static final String HELM_DOCKER_IMAGE_NAME_PLACEHOLDER = "${DOCKER_IMAGE_NAME}";
  public static final String HELM_DOCKER_IMAGE_TAG_PLACEHOLDER = "${DOCKER_IMAGE_TAG}";

  public static final String KUBE_CONFIG_TEMPLATE = "apiVersion: v1\n"
      + "clusters:\n"
      + "- cluster:\n"
      + "    xxxxxxxx ${MASTER_URL}\n"
      + "    insecure-skip-tls-verify: true\n"
      + "  name: CLUSTER_NAME\n"
      + "contexts:\n"
      + "- context:\n"
      + "    cluster: CLUSTER_NAME\n"
      + "    user: HARNESS_USER\n"
      + "    ${NAMESPACE}\n"
      + "  name: CURRENT_CONTEXT\n"
      + "current-context: CURRENT_CONTEXT\n"
      + "kind: Config\n"
      + "preferences: {}\n"
      + "users:\n"
      + "- name: HARNESS_USER\n"
      + "  user:\n"
      + "    ${CLIENT_CERT_DATA}\n"
      + "    ${CLIENT_KEY_DATA}\n"
      + "    ${PASSWORD}\n"
      + "    ${USER_NAME}\n"
      + "    ${SERVICE_ACCOUNT_TOKEN_DATA}";

  public static final String HELM_ROLLBACK_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm rollback ${COMMAND_FLAGS} ${RELEASE} ${REVISION}";
  public static final String HELM_INSTALL_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm install ${COMMAND_FLAGS} ${CHART_REFERENCE} ${OVERRIDE_VALUES} ${RELEASE_NAME} ${NAMESPACE}";
  public static final String HELM_UPGRADE_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm upgrade ${COMMAND_FLAGS} ${RELEASE_NAME} ${CHART_REFERENCE} ${OVERRIDE_VALUES}";

  // The reason we are using ^ and $ before and after ${RELEASE_NAME} is because helm list doesn't take releaseName as a
  // param and release name becomes a regex
  public static final String HELM_LIST_RELEASE_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm list ${COMMAND_FLAGS} ^${RELEASE_NAME}$";
  public static final String HELM_RELEASE_HIST_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm hist ${COMMAND_FLAGS} ${RELEASE_NAME} ${FLAGS}";
  public static final String HELM_VERSION_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm version ${COMMAND_FLAGS}";
  public static final String HELM_ADD_REPO_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm repo add ${REPO_NAME} ${REPO_URL}";
  public static final String HELM_REPO_LIST_COMMAND_TEMPLATE = "KUBECONFIG=${KUBECONFIG_PATH} helm repo list";
  public static final String HELM_DELETE_RELEASE_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm delete ${COMMAND_FLAGS} ${FLAGS} ${RELEASE_NAME}";

  public static final long DEFAULT_HELM_COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
  public static final String DEFAULT_TILLER_CONNECTION_TIMEOUT_SECONDS = "60"; // seconds

  public static final String DEFAULT_HELM_VALUE_YAML = "# Enter your Helm value YAML\n"
      + "#\n"
      + "# Placeholders:\n"
      + "#\n"
      + "# Optional: ${NAMESPACE}\n"
      + "#   - Replaced with the namespace\n"
      + "#     Harness will set the namespace from infrastructure\n"
      + "#     mapping namespace\n"
      + "#\n"
      + "# Optional: ${DOCKER_IMAGE_NAME}\n"
      + "#   - Replaced with the Docker image name\n"
      + "#\n"
      + "# Optional: ${DOCKER_IMAGE_TAG}\n"
      + "#   - Replaced with the Docker image tag\n"
      + "#\n"
      + "# ---\n"
      + "namespace : ${NAMESPACE}\n";
}
