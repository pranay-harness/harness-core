package io.harness.connector;

import com.google.inject.AbstractModule;
import io.harness.connector.task.git.NGGitService;
import io.harness.connector.task.git.NGGitServiceImpl;
import io.harness.git.GitClientV2;
import io.harness.git.GitClientV2Impl;


import java.util.concurrent.atomic.AtomicReference;


public class ConnectorTaskModule extends AbstractModule {
    private static volatile ConnectorTaskModule instance;

    private static final AtomicReference<ConnectorTaskModule> instanceRef = new AtomicReference();

    public ConnectorTaskModule() {}

    @Override
    protected void configure() {
        bind(NGGitService.class).to(NGGitServiceImpl.class);
        bind(GitClientV2.class).to(GitClientV2Impl.class);
    }

    public static ConnectorTaskModule getInstance() {
        if (instanceRef.get() == null) {
            instanceRef.compareAndSet(null, new ConnectorTaskModule());
        }
        return instanceRef.get();
    }
}