package io.harness.grpc.utils;

import static io.harness.configuration.DeployMode.DEPLOY_MODE;
import static io.harness.configuration.DeployMode.isOnPrem;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.experimental.UtilityClass;

/**
 * Utility class for the methods to extract grpc target & authority from manager url. Used to provide values while
 * processing delegate config templates.
 */
@UtilityClass
public class DelegateGrpcConfigExtractor {
  private static String MANAGER_GRPC_PORT = "9879";
  public static String extractTarget(String managerUrl) {
    try {
      if ("KUBERNETES_ONPREM".equals(System.getenv().get(DEPLOY_MODE))) {
        return new URI(managerUrl).getAuthority() + ":" + MANAGER_GRPC_PORT;
      }
      return new URI(managerUrl).getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }

  public static String extractAuthority(String managerUrl, String svc) {
    if (isOnPrem(System.getenv().get(DEPLOY_MODE))) {
      return "default-authority.harness.io";
    }
    try {
      URI uri = new URI(managerUrl);
      String path = uri.getPath();
      String[] parts = path.split("/");
      String prefix = null;
      if (parts.length > 1) {
        prefix = parts[1];
      }
      return Stream.of(prefix, svc, "grpc", uri.getAuthority())
          .filter(Objects::nonNull)
          .collect(Collectors.joining("-"));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }

  public static String extractScheme(String managerUrl) {
    try {
      return new URI(managerUrl).getScheme();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }
}
