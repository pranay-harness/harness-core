package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.acl.models.ACL;
import io.harness.accesscontrol.acl.models.SourceMetadata;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class ACLRepositoryCustomImpl implements ACLRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private static final String ROLE_IDENTIFIER_KEY =
      ACL.ACLKeys.sourceMetadata + "." + SourceMetadata.SourceMetadataKeys.roleIdentifier;
  private static final String RESOURCE_GROUP_IDENTIFIER_KEY =
      ACL.ACLKeys.sourceMetadata + "." + SourceMetadata.SourceMetadataKeys.resourceGroupIdentifier;

  @Override
  public List<ACL> findByRole(String scopeIdentifier, String identifier) {
    return mongoTemplate.find(
        new Query(
            Criteria.where(ACL.ACLKeys.scopeIdentifier).is(scopeIdentifier).and(ROLE_IDENTIFIER_KEY).is(identifier)),
        ACL.class);
  }

  @Override
  public void deleteAll(Criteria criteria) {
    mongoTemplate.remove(new Query(criteria), ACL.class);
  }

  @Override
  public List<ACL> findByResourceGroup(String scopeIdentifier, String identifier) {
    return mongoTemplate.find(new Query(Criteria.where(ACL.ACLKeys.scopeIdentifier)
                                            .is(scopeIdentifier)
                                            .and(RESOURCE_GROUP_IDENTIFIER_KEY)
                                            .is(identifier)),
        ACL.class);
  }
}
