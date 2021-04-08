package io.harness.ng.core;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.AccessControlAdminClient;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.NextGenConfiguration;
import io.harness.ng.core.invites.api.InviteService;
import io.harness.ng.core.invites.api.impl.InviteServiceImpl;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.ng.core.user.service.impl.NgUserServiceImpl;
import io.harness.persistence.HPersistence;
import io.harness.remote.client.ServiceHttpClientConfig;
import io.harness.user.UserClientModule;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import org.springframework.data.mongodb.MongoTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@OwnedBy(PL)
public class InviteModule extends AbstractModule {
  private final ServiceHttpClientConfig serviceHttpClientConfig;
  private final String managerServiceSecret;
  private final String clientId;

  public InviteModule(ServiceHttpClientConfig serviceHttpClientConfig, String managerServiceSecret, String clientId) {
    this.serviceHttpClientConfig = serviceHttpClientConfig;
    this.managerServiceSecret = managerServiceSecret;
    this.clientId = clientId;
  }

  @Override
  protected void configure() {
    bind(InviteService.class).to(InviteServiceImpl.class);
    bind(NgUserService.class).to(NgUserServiceImpl.class);
    registerRequiredBindings();
    install(UserClientModule.getInstance(serviceHttpClientConfig, managerServiceSecret, clientId));
  }

  @Provides
  @Named("userVerificationSecret")
  @Singleton
  protected String getUserVerificationSecret(NextGenConfiguration nextGenConfiguration) {
    return nextGenConfiguration.getNextGenConfig().getUserVerificationSecret();
  }

  @Provides
  @Singleton
  protected TransactionTemplate getTransactionTemplate(MongoTransactionManager mongoTransactionManager) {
    return new TransactionTemplate(mongoTransactionManager);
  }

  private void registerRequiredBindings() {
    requireBinding(HPersistence.class);
    requireBinding(AccessControlAdminClient.class);
  }
}
