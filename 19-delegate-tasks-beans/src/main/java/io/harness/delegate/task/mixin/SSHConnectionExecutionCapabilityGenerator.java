package io.harness.delegate.task.mixin;

import io.harness.delegate.beans.executioncapability.SocketConnectivityExecutionCapability;
import io.harness.expression.DummySubstitutor;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class SSHConnectionExecutionCapabilityGenerator {
  public static SocketConnectivityExecutionCapability buildSSHConnectionExecutionCapability(String urlString) {
    try {
      SSHUriParser sshUriParser = SSHUriParser.parse(DummySubstitutor.substitute(urlString));

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
        sshUriParser.user = m.group(2).replace("@", "");
        sshUriParser.host = m.group(3).replace("://", "");
        sshUriParser.port = m.group(4) == null ? "22" : m.group(4);
        sshUriParser.path = m.group(5);
        return sshUriParser;
      } else {
        return null;
      }
    }
  }
}
