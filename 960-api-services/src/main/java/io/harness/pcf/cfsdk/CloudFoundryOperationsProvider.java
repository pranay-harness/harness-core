package io.harness.pcf.cfsdk;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.harness.annotations.dev.OwnedBy;
import io.harness.pcf.PivotalClientApiException;
import io.harness.pcf.model.CfRequestConfig;
import lombok.extern.slf4j.Slf4j;
import org.cloudfoundry.operations.CloudFoundryOperations;
import org.cloudfoundry.operations.DefaultCloudFoundryOperations;
import org.cloudfoundry.reactor.ConnectionContext;

import static io.harness.annotations.dev.HarnessTeam.CDP;

@Singleton
@Slf4j
@OwnedBy(CDP)
public class CloudFoundryOperationsProvider {
  @Inject private ConnectionContextProvider connectionContextProvider;
  @Inject private CloudFoundryClientProvider cloudFoundryClientProvider;

  public CloudFoundryOperationsWrapper getCloudFoundryOperationsWrapper(CfRequestConfig pcfRequestConfig)
      throws PivotalClientApiException {
    try {
      ConnectionContext connectionContext = connectionContextProvider.getConnectionContext(pcfRequestConfig);
      CloudFoundryOperations cloudFoundryOperations =
          DefaultCloudFoundryOperations.builder()
              .cloudFoundryClient(cloudFoundryClientProvider.getCloudFoundryClient(pcfRequestConfig, connectionContext))
              .organization(pcfRequestConfig.getOrgName())
              .space(pcfRequestConfig.getSpaceName())
              .build();

      return CloudFoundryOperationsWrapper.builder()
          .cloudFoundryOperations(cloudFoundryOperations)
          .connectionContext(connectionContext)
          .ignorePcfConnectionContextCache(pcfRequestConfig.isIgnorePcfConnectionContextCache())
          .build();
    } catch (Exception e) {
      throw new PivotalClientApiException("Exception while creating CloudFoundryOperations: " + e.getMessage(), e);
    }
  }
}
