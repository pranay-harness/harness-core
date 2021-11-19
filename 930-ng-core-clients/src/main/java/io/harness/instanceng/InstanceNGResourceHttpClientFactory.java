package io.harness.instanceng;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

@OwnedBy(HarnessTeam.CE)
public class InstanceNGResourceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<InstanceNGResourceClient> {
  public InstanceNGResourceHttpClientFactory(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator serviceTokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      ClientMode clientMode) {
    super(
        ngManagerClientConfig, serviceSecret, serviceTokenGenerator, kryoConverterFactory, clientId, false, clientMode);
  }

  @Override
  public InstanceNGResourceClient get() {
    return getRetrofit().create(InstanceNGResourceClient.class);
  }
}
