package io.harness.network;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class NoopHostnameVerifier implements HostnameVerifier {
  @Override
  public boolean verify(String hostname, SSLSession session) {
    log.info("Approving hostname " + hostname);
    return true;
  }
}
