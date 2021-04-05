package io.harness.beans.shared;

import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;

import static org.springframework.data.mongodb.core.query.Query.query;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.shared.ResourceConstraint.ResourceConstraintKeys;
import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.validation.Create;

import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

@OwnedBy(HarnessTeam.PIPELINE)
public class RestraintServiceImpl implements RestraintService {
  @Inject private HPersistence hPersistence;
  @Inject private MongoTemplate mongoTemplate;

  @Override
  public List<ResourceConstraint> getConstraintsIn(Set<String> constraintIds) {
    try (HIterator<ResourceConstraint> iterator =
             new HIterator<>(hPersistence.createQuery(ResourceConstraint.class, excludeAuthority)
                                 .field(ResourceConstraintKeys.uuid)
                                 .in(constraintIds)
                                 .fetch())) {
      return IteratorUtils.toList(iterator.iterator());
    }
  }

  @Override
  public ResourceConstraint get(String accountId, String resourceConstraintId) {
    final ResourceConstraint resourceConstraint = hPersistence.get(ResourceConstraint.class, resourceConstraintId);
    if (resourceConstraint != null && accountId != null && !resourceConstraint.getAccountId().equals(accountId)) {
      return null;
    }
    return resourceConstraint;
  }

  @Override
  public ResourceConstraint getByNameAndAccountId(String name, String accountId) {
    Criteria nameCriteria = Criteria.where(ResourceConstraintKeys.name).is(name);
    Criteria accountIdCriteria = Criteria.where(ResourceConstraintKeys.accountId).is(accountId);

    Query query = query(new Criteria().andOperator(nameCriteria, accountIdCriteria));

    return mongoTemplate.findOne(query, ResourceConstraint.class);
  }

  @Override
  @ValidationGroups(Create.class)
  public ResourceConstraint save(ResourceConstraint resourceConstraint) {
    try {
      hPersistence.save(resourceConstraint);
      return resourceConstraint;
    } catch (DuplicateKeyException exception) {
      throw new InvalidRequestException("The resource constraint name cannot be reused.", exception, USER);
    }
  }
}
