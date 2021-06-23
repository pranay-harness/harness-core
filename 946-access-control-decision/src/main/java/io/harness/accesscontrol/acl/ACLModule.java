package io.harness.accesscontrol.acl;

import io.harness.accesscontrol.acl.daos.ACLDAO;
import io.harness.accesscontrol.acl.daos.ACLDAOImpl;
import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.repositories.PrimaryACLRepositoryImpl;
import io.harness.accesscontrol.acl.repositories.SecondaryACLRepositoryImpl;
import io.harness.accesscontrol.acl.repository.ACLRepository;
import io.harness.accesscontrol.acl.services.ACLService;
import io.harness.accesscontrol.acl.services.ACLServiceImpl;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

@OwnedBy(HarnessTeam.PL)
public class ACLModule extends AbstractModule {
  private static ACLModule instance;

  public static ACLModule getInstance() {
    if (instance == null) {
      instance = new ACLModule();
    }
    return instance;
  }

  @Override
  protected void configure() {
    bind(ACLService.class).to(ACLServiceImpl.class);
    bind(ACLDAO.class).to(ACLDAOImpl.class);
    bind(ACLRepository.class).annotatedWith(Names.named(ACL.PRIMARY_COLLECTION)).to(PrimaryACLRepositoryImpl.class);
    bind(ACLRepository.class).annotatedWith(Names.named(ACL.SECONDARY_COLLECTION)).to(SecondaryACLRepositoryImpl.class);
  }
}
