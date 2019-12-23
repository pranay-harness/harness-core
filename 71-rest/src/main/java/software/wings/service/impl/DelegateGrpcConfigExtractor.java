package software.wings.service.impl;

import com.google.common.base.Joiner;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for the methods to extract grpc target & authority from manager url. Used to provide values while
 * processing delegate config templates.
 */
@UtilityClass
public class DelegateGrpcConfigExtractor {
  private static final Joiner JOINER = Joiner.on("-").skipNulls();

  public static String extractTarget(String managerUrl) {
    try {
      return new URI(managerUrl).getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl);
    }
  }

  public static String extractAuthority(String managerUrl, String svc) {
    try {
      URI uri = new URI(managerUrl);
      String path = uri.getPath();
      String[] parts = path.split("/");
      String namespace = null;
      if (parts.length > 1) {
        namespace = parts[1];
      }
      return JOINER.join(namespace, svc, "grpc", uri.getAuthority());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl);
    }
  }
}
