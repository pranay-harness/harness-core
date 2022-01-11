package io.harness.gitops;

import io.harness.gitops.remote.GitopsResourceClient;
import io.harness.gitops.remote.GitopsResourceClientHttpFactory;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.security.ServiceTokenGenerator;
import io.harness.serializer.kryo.KryoConverterFactory;
import io.harness.template.remote.TemplateResourceClient;
import io.harness.template.remote.TemplateResourceClientHttpFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provides;
import com.google.inject.Scopes;

public class GitopsResourceClientModule extends AbstractModule {
    private final ServiceHttpClientConfig ngManagerClientConfig;
    private final String serviceSecret;
    private final String clientId;

    @Inject
    public GitopsResourceClientModule(
            ServiceHttpClientConfig ngManagerClientConfig, String serviceSecret, String clientId) {
        this.ngManagerClientConfig = ngManagerClientConfig;
        this.serviceSecret = serviceSecret;
        this.clientId = clientId;
    }

    @Provides
    private TemplateResourceClientHttpFactory templateResourceClientHttpFactory(
            KryoConverterFactory kryoConverterFactory) {
        return new TemplateResourceClientHttpFactory(
                this.ngManagerClientConfig, this.serviceSecret, new ServiceTokenGenerator(), kryoConverterFactory, clientId);
    }

    @Override
    protected void configure() {
        this.bind(GitopsResourceClient.class).toProvider(GitopsResourceClientHttpFactory.class).in(Scopes.SINGLETON);
    }

}
