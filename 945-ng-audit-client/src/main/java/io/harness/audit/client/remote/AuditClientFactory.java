package io.harness.audit.client.remote;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.remote.client.AbstractHttpClientFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.Provider;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE)
@Singleton
public class AuditClientFactory extends AbstractHttpClientFactory implements Provider<AuditClient> {
  public AuditClientFactory(ServiceHttpClientConfig auditConfig, String serviceSecret,
      ServiceTokenGenerator tokenGenerator, KryoConverterFactory kryoConverterFactory, String clientId) {
    super(auditConfig, serviceSecret, tokenGenerator, kryoConverterFactory, clientId);
  }

  @Override
  public AuditClient get() {
    return getRetrofit().create(AuditClient.class);
  }
}
