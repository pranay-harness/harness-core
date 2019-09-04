package software.wings.service.impl;

import lombok.experimental.UtilityClass;

import java.net.URI;
import java.net.URISyntaxException;

/**
 * Utility class for the methods to extract grpc target & authority from manager url.
 */
@UtilityClass
class EventsCollectionConfigExtractor {
  static String extractPublishTarget(String managerUrl) {
    try {
      return new URI(managerUrl).getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl);
    }
  }

  static String extractPublishAuthority(String managerUrl) {
    try {
      URI uri = new URI(managerUrl);
      String path = uri.getPath();
      String[] parts = path.split("/");
      String prefix = "";
      if (parts.length > 1) {
        prefix = parts[1] + "-";
      }
      return prefix + "events-" + uri.getAuthority();
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid manager url " + managerUrl);
    }
  }
}
