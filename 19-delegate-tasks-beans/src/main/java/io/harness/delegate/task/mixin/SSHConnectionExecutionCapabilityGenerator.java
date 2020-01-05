package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.expression.DummySubstitutor;
import lombok.Getter;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@UtilityClass
@Slf4j
public class SSHConnectionExecutionCapabilityGenerator {
  public static SocketConnectivityExecutionCapability buildSSHConnectionExecutionCapability(String urlString) {
    try {
      SSHUriParser sshUriParser = SSHUriParser.parse(DummySubstitutor.substitute(sanitizeUrl(urlString)));

      if (sshUriParser != null) {
        return SocketConnectivityExecutionCapability.builder()
            .scheme(sshUriParser.getScheme())
            .hostName(sshUriParser.getHost())
            .port(sshUriParser.getPort())
            .url(urlString)
            .build();
      } else {
        return SocketConnectivityExecutionCapability.builder()
            .url(urlString)
            .scheme(null)
            .port(null)
            .hostName(null)
            .build();
      }
    } catch (Exception e) {
      logger.warn("conversion to java.net.URI failed for url: " + urlString);
      // This is falling back to existing approach, where we test for entire URL
      return SocketConnectivityExecutionCapability.builder()
          .url(urlString)
          .scheme(null)
          .port(null)
          .hostName(null)
          .build();
    }
  }

  @Getter
  static class SSHUriParser {
    private String scheme;
    private String host;
    private String port;
    private String user;
    private String path;
    private String url;

    public static SSHUriParser parse(String url) {
      if (!url.contains("://") && url.contains("@")) {
        url = "ssh://" + url;
      }
      Pattern pattern = Pattern.compile("(\\w+://)(.+@)*([\\w\\d\\.]+):([\\d]+){0,1}/*(.*)");
      Matcher m = pattern.matcher(url);
      if (m.matches()) {
        SSHUriParser sshUriParser = new SSHUriParser();
        sshUriParser.url = url;
        sshUriParser.scheme = m.group(1).replace("://", "");
        sshUriParser.user = m.group(2) != null ? m.group(2).replace("@", "") : null;
        sshUriParser.host = m.group(3) != null ? m.group(3).replace("://", "") : null;
        sshUriParser.port = m.group(4) == null ? "22" : m.group(4);
        sshUriParser.path = m.group(5);
        return sshUriParser;
      } else {
        try {
          URI uri = new URI(url);
          SSHUriParser sshUriParser = new SSHUriParser();
          sshUriParser.host = uri.getHost();
          sshUriParser.scheme = uri.getScheme();
          sshUriParser.path = uri.getPath();
          sshUriParser.port = uri.getPort() != -1 ? "" + uri.getPort() : "22";
          return sshUriParser;
        } catch (URISyntaxException e) {
          return null;
        }
      }
    }
  }

  private static String sanitizeUrl(String urlString) {
    String sanitizedUrl = urlString.replaceAll("\\\\", "/");
    return sanitizedUrl.trim();
  }
}
