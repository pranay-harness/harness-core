package io.harness;

import com.google.inject.AbstractModule;

import io.harness.managerclient.ManagerCIResource;
import io.harness.managerclient.ManagerClientFactory;
import io.harness.security.ServiceTokenGenerator;

public class CIExecutionTestRule extends AbstractModule {
  @Override
  protected void configure() {
    ServiceTokenGenerator tokenGenerator = new ServiceTokenGenerator();
    bind(ServiceTokenGenerator.class).toInstance(tokenGenerator);
    bind(ManagerCIResource.class)
        .toProvider(new ManagerClientFactory("https://localhost:9090/api"
                + "/",
            tokenGenerator));
  }
}
