package io.harness.istio.api.networking;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.k8s.model.K8sExpressions.canaryDestinationExpression;
import static io.harness.k8s.model.K8sExpressions.stableDestinationExpression;

import static java.lang.String.format;
import static me.snowdrop.istio.api.internal.IstioSpecRegistry.getCRDInfo;
import static org.apache.commons.lang3.StringUtils.EMPTY;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.k8s.model.HarnessLabelValues;

import io.fabric8.kubernetes.api.model.apiextensions.CustomResourceDefinition;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.extern.slf4j.Slf4j;
import me.snowdrop.istio.api.IstioResource;
import me.snowdrop.istio.api.internal.IstioSpecRegistry;

@Slf4j
@OwnedBy(HarnessTeam.CDP)
public class IstioApiNetworkingHandlerHelper {
  public static final String ISTIO_DESTINATION_TEMPLATE = "host: $ISTIO_DESTINATION_HOST_NAME\n"
      + "subset: $ISTIO_DESTINATION_SUBSET_NAME";

  public static String generateDestination(String host, String subset) {
    return ISTIO_DESTINATION_TEMPLATE.replace("$ISTIO_DESTINATION_HOST_NAME", host)
        .replace("$ISTIO_DESTINATION_SUBSET_NAME", subset);
  }

  public static String getDestinationYaml(String destination, String host) {
    if (canaryDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackCanary);
    } else if (stableDestinationExpression.equals(destination)) {
      return generateDestination(host, HarnessLabelValues.trackStable);
    } else {
      return destination;
    }
  }

  public static CustomResourceDefinition getCustomResourceDefinition(KubernetesClient client, IstioResource resource) {
    final String crdName = getCRDNameFor(resource.getKind(), resource.getApiVersion());
    final CustomResourceDefinition customResourceDefinition =
        client.customResourceDefinitions().withName(crdName).get();
    if (customResourceDefinition == null) {
      throw new IllegalArgumentException(
          format("Custom Resource Definition %s is not found in cluster %s", crdName, client.getMasterUrl()));
    }
    return customResourceDefinition;
  }

  public static String getCRDNameFor(String kind, String version) {
    IstioSpecRegistry.CRDInfo crdInfo =
        getCRDInfo(kind, version.substring(version.lastIndexOf("/") + 1))
            .orElseThrow(
                () -> new IllegalArgumentException(format("%s/%s is not a known Istio resource.", kind, version)));

    String crdNameSuffix = crdInfo.getAPIVersion().lastIndexOf("/") == -1
        ? EMPTY
        : crdInfo.getAPIVersion().substring(0, crdInfo.getAPIVersion().lastIndexOf("/"));
    return isEmpty(crdNameSuffix) ? EMPTY : format("%s.%s", crdInfo.getPlural(), crdNameSuffix);
  }
}
