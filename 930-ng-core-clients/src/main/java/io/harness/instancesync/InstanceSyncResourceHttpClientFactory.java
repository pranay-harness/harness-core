package io.harness.instancesync;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ClientMode;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;

@OwnedBy(HarnessTeam.DX)
public class InstanceSyncResourceHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<InstanceSyncResourceClient> {
  public InstanceSyncResourceHttpClientFactory(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator serviceTokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId,
      ClientMode clientMode) {
    super(
        ngManagerClientConfig, serviceSecret, serviceTokenGenerator, kryoConverterFactory, clientId, false, clientMode);
  }

  @Override
  public InstanceSyncResourceClient get() {
    return getRetrofit().create(InstanceSyncResourceClient.class);
  }
}
