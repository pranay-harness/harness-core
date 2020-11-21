package io.harness.projectmanagerclient.remote;

import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class ProjectManagerHttpClientFactory
    extends AbstractHttpClientFactory implements Provider<ProjectManagerClient> {
  public ProjectManagerHttpClientFactory(ServiceHttpClientConfig projectManagerClientConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(projectManagerClientConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public ProjectManagerClient get() {
    return getRetrofit().create(ProjectManagerClient.class);
  }
}
