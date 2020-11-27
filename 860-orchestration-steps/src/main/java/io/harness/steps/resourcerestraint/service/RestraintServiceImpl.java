package io.harness.steps.resourcerestraint.service;

import static io.harness.exception.WingsException.USER;
import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.exception.InvalidRequestException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;
import io.harness.steps.resourcerestraint.beans.ResourceConstraint;
import io.harness.steps.resourcerestraint.beans.ResourceConstraint.ResourceConstraintKeys;
import io.harness.validation.Create;

import com.google.inject.Inject;
import com.mongodb.DuplicateKeyException;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.IteratorUtils;
import ru.vyarus.guice.validator.group.annotation.ValidationGroups;

public class RestraintServiceImpl implements RestraintService {
  @Inject private HPersistence hPersistence;

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
