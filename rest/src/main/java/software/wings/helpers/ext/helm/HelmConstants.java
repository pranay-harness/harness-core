package software.wings.helpers.ext.helm;

import java.util.concurrent.TimeUnit;

/**
 * Created by anubhaw on 3/23/18.
 */
public class HelmConstants {
  public static final String HELM_ROLLBACK_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm rollback ${RELEASE} ${REVISION}";
  public static final String HELM_INSTALL_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm install ${CHART_REFERENCE} ${OVERRIDE_VALUES} ${RELEASE_NAME} ${NAMESPACE} ${TIMEOUT}";
  public static final String HELM_UPGRADE_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm upgrade ${RELEASE_NAME} ${CHART_REFERENCE} ${OVERRIDE_VALUES} ${TIMEOUT}";
  public static final String HELM_LIST_RELEASE_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm list ${FLAGS} ${RELEASE_NAME}";
  public static final String HELM_RELEASE_HIST_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm hist ${RELEASE_NAME} ${FLAGS}";
  public static final String HELM_VERSION_COMMAND_TEMPLATE =
      "KUBECONFIG=${KUBECONFIG_PATH} helm version  --tiller-connection-timeout ${TILLER_CONN_TIMEOUT}";

  public static final long DEFAULT_HELM_COMMAND_TIMEOUT = TimeUnit.MINUTES.toMillis(30);
  public static final String DEFAULT_TILLER_CONNECTION_TIMEOUT = "60"; // seconds
}
