package io.harness.pcf.cfsdk;

import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.ExceptionUtils;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.client.CloudFoundryClient;
import org.cloudfoundry.reactor.ConnectionContext;
import org.cloudfoundry.reactor.TokenProvider;
import org.cloudfoundry.reactor.client.ReactorCloudFoundryClient;
import org.cloudfoundry.reactor.tokenprovider.PasswordGrantTokenProvider;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class CloudFoundryClientProvider {
  public CloudFoundryClient getCloudFoundryClient(
      CfRequestConfig pcfRequestConfig, ConnectionContext connectionContext) throws PivotalClientApiException {
    return ReactorCloudFoundryClient.builder()
        .connectionContext(connectionContext)
        .tokenProvider(getTokenProvider(pcfRequestConfig.getUserName(), pcfRequestConfig.getPassword()))
        .build();
  }

  private TokenProvider getTokenProvider(String username, String password) throws PivotalClientApiException {
    try {
      return PasswordGrantTokenProvider.builder().username(username).password(password).build();
    } catch (Exception t) {
      throw new PivotalClientApiException(ExceptionUtils.getMessage(t));
    }
  }
}
