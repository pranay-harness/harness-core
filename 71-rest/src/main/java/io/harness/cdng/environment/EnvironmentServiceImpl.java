package io.harness.cdng.environment;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.cdng.common.beans.Tag;
import io.harness.cdng.environment.beans.Environment;
import io.harness.cdng.environment.beans.Environment.EnvironmentKeys;
import io.harness.exception.InvalidRequestException;
import software.wings.dl.WingsPersistence;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

@Singleton
public class EnvironmentServiceImpl implements EnvironmentService {
  @Inject WingsPersistence wingsPersistence;

  public void save(Environment environment) {
    validate(environment);
    if (isEmpty(environment.getDisplayName())) {
      environment.setDisplayName(environment.getIdentifier());
    }
    wingsPersistence.save(environment);
  }

  private void validate(Environment environment) {
    notNullCheck("Environment Type can't be empty", environment.getEnvironmentType());
    notNullCheck("Environment identifier can't be empty", environment.getIdentifier());
  }

  public void update(Environment savedEnvironment, Environment environment) {
    validateImmutableFields(savedEnvironment, environment);

    Map<String, Object> updateValues = new HashMap<>();

    if (isNotEmpty(environment.getTags())) {
      List<Tag> aggregatedTags = getAggregatedTags(savedEnvironment.getTags(), environment.getTags());
      updateValues.put(EnvironmentKeys.tags, aggregatedTags);
    }

    if (isNotEmpty(environment.getDisplayName())
        && !environment.getDisplayName().equals(savedEnvironment.getDisplayName())) {
      updateValues.put(EnvironmentKeys.displayName, environment.getDisplayName());
    }

    if (isNotEmpty(updateValues)) {
      wingsPersistence.updateFields(Environment.class, savedEnvironment.getUuid(), updateValues);
    }
  }

  private List<Tag> getAggregatedTags(List<Tag> oldTags, List<Tag> newTags) {
    // TODO: implement later
    ArrayList<Tag> aggregatedTags = new ArrayList<>();
    if (isNotEmpty(oldTags)) {
      aggregatedTags.addAll(oldTags);
    }
    aggregatedTags.addAll(newTags);
    return aggregatedTags;
  }

  private void validateImmutableFields(Environment savedEnvironment, Environment environment) {
    if (environment.getEnvironmentType() != null
        && savedEnvironment.getEnvironmentType() != environment.getEnvironmentType()) {
      throw new InvalidRequestException(String.format("Environment type is immutable. Existing: [%s], New: [%s]",
          savedEnvironment.getEnvironmentType(), environment.getEnvironmentType()));
    }
  }

  @Override
  public void upsert(@Nonnull Environment environment) {
    Environment savedEnvironment = wingsPersistence.createQuery(Environment.class)
                                       .filter(EnvironmentKeys.accountId, environment.getAccountId())
                                       .filter(EnvironmentKeys.orgId, environment.getOrgId())
                                       .filter(EnvironmentKeys.projectId, environment.getProjectId())
                                       .filter(EnvironmentKeys.identifier, environment.getIdentifier())
                                       .get();

    if (savedEnvironment == null) {
      save(environment);
    } else {
      update(savedEnvironment, environment);
    }
  }
}
