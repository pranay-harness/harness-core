package io.harness.accesscontrol.acl.repository;

import io.harness.accesscontrol.HPrincipal;
import io.harness.accesscontrol.Principal;
import io.harness.accesscontrol.acl.models.HACL.HACLKeys;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@AllArgsConstructor(access = AccessLevel.PRIVATE, onConstructor = @__({ @Inject }))
@Singleton
@Slf4j
public class HACLRepositoryCustomImpl implements ACLRepositoryCustom {
  private final MongoTemplate mongoTemplate;
  private static final String PRINCIPAL_TYPE_KEY = HACLKeys.principal + "." + HPrincipal.PrincipalKeys.principalType;
  private static final String PRINCIPAL_IDENTIFIER_KEY =
      HACLKeys.principal + "." + HPrincipal.PrincipalKeys.principalIdentifier;

  @Override
  public void deleteByPrincipal(Principal principal) {
    HPrincipal hPrincipal = (HPrincipal) principal;
    mongoTemplate.remove(new Query(Criteria.where(PRINCIPAL_TYPE_KEY)
                                       .is(hPrincipal.getPrincipalType())
                                       .and(PRINCIPAL_IDENTIFIER_KEY)
                                       .is(hPrincipal.getPrincipalIdentifier())));
  }
}
