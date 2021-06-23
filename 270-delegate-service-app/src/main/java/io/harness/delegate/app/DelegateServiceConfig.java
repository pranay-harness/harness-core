package io.harness.delegate.app;

import static io.harness.annotations.dev.HarnessTeam.DEL;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.grpc.server.GrpcServerConfig;

import software.wings.app.MainConfiguration;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Singleton;
import io.dropwizard.bundles.assets.AssetsBundleConfiguration;
import io.dropwizard.jetty.ConnectorFactory;
import io.dropwizard.jetty.HttpConnectorFactory;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Used to load all the delegate mgr configuration.
 */
@TargetModule(HarnessModule._420_DELEGATE_SERVICE)
@Data
@EqualsAndHashCode(callSuper = false)
@Singleton
@OwnedBy(DEL)
public class DelegateServiceConfig extends MainConfiguration implements AssetsBundleConfiguration {
  @JsonProperty("delegateServiceUrl") private String delegateServiceUrl;
  @JsonProperty("grpcServerClassicConfig") private GrpcServerConfig grpcServerClassicConfig;
  @JsonProperty("delegateServiceSecret") private String delegateServiceSecret;

  private static final String IS_OPTION_HEAD_HTTP_METHOD_BLOCKED = "IS_OPTION_HEAD_REQUEST_METHOD_BLOCKED";

  /**
   * Instantiates Delegate Service Configuration.
   */
  public DelegateServiceConfig() {
    super();
  }

  protected ConnectorFactory getDefaultAdminConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(9081);
    return factory;
  }

  protected ConnectorFactory getDefaultApplicationConnectorFactory() {
    final HttpConnectorFactory factory = new HttpConnectorFactory();
    factory.setPort(9080);
    return factory;
  }
}
