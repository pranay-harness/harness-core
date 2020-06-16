package io.harness.grpc.utils;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Utility class for the methods to extract grpc target & authority from manager url. Used to provide values while
 * processing delegate config templates.
 */
@UtilityClass
public class DelegateGrpcConfigExtractor {
  public static String extractTarget(String managerUrl) {
    try {
      return new URI(managerUrl).getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl, e);
    }
  }

  public static String extractAuthority(String managerUrl, String svc) {
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
}
