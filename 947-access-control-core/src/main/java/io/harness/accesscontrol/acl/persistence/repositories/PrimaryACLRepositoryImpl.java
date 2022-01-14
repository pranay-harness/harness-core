package io.harness.accesscontrol.acl.persistence.repositories;

import io.harness.accesscontrol.acl.persistence.ACL;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.springframework.data.mongodb.core.MongoTemplate;

@OwnedBy(HarnessTeam.PL)
@Singleton
public class PrimaryACLRepositoryImpl extends BaseACLRepositoryImpl implements ACLRepository {
  @Inject
  public PrimaryACLRepositoryImpl(MongoTemplate mongoTemplate) {
    super(mongoTemplate);
  }

  @Override
  protected String getCollectionName() {
    return ACL.PRIMARY_COLLECTION;
  }
}
