package io.harness.feature.client;

import io.harness.feature.client.annotation.FeatureRestrictionCheck;
import io.harness.feature.client.annotation.interceptor.FeatureCheckInterceptor;
import io.harness.feature.client.services.PlanFeatureClientService;
import io.harness.feature.client.services.PlanFeatureRegisterService;
import io.harness.feature.client.services.impl.PlanFeatureClientServiceImpl;
import io.harness.feature.client.services.impl.PlanFeatureRegisterServiceImpl;
import io.harness.govern.ProviderMethodInterceptor;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.matcher.Matchers;

public class PlanFeatureClientModule extends AbstractModule {
  private static PlanFeatureClientModule instance;
  private final ServiceHttpClientConfig ngManagerClientConfig;
  private final String serviceSecret;
  private final String clientId;
  private final PlanFeatureClientConfiguration planFeatureClientConfiguration;

  private PlanFeatureClientModule(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId,
      PlanFeatureClientConfiguration planFeatureClientConfiguration) {
    this.ngManagerClientConfig = ngManagerClientConfig;
    this.serviceSecret = serviceSecret;
    this.clientId = clientId;
    this.planFeatureClientConfiguration = planFeatureClientConfiguration;
  }

  public static PlanFeatureClientModule getInstance(ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret,
      String clientId, PlanFeatureClientConfiguration planFeatureClientConfiguration) {
    if (instance == null) {
      instance =
          new PlanFeatureClientModule(ngManagerClientConfig, serviceSecret, clientId, planFeatureClientConfiguration);
    }
    return instance;
  }

  @Provides
  private PlanFeatureClientFactory planFeatureClientFactory(KryoConverterFactory kryoConverterFactory) {
    return new PlanFeatureClientFactory(
        this.ngManagerClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
  }

  @Provides
  private PlanFeatureClientConfiguration planFeatureClientConfiguration() {
    return planFeatureClientConfiguration;
  }

  @Override
  protected void configure() {
    ProviderMethodInterceptor featureCheck = new ProviderMethodInterceptor(getProvider(FeatureCheckInterceptor.class));
    bindInterceptor(Matchers.any(), Matchers.annotatedWith(FeatureRestrictionCheck.class), featureCheck);

    bind(PlanFeatureClient.class).toProvider(PlanFeatureClientFactory.class).in(Scopes.SINGLETON);
    bind(PlanFeatureClientService.class).to(PlanFeatureClientServiceImpl.class);
    bind(PlanFeatureRegisterService.class).to(PlanFeatureRegisterServiceImpl.class);
  }
}
