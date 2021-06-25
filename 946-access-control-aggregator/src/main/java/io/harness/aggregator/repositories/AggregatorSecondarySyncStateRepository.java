package io.harness.aggregator.repositories;

import io.harness.aggregator.models.AggregatorSecondarySyncState;
import io.harness.aggregator.models.AggregatorSecondarySyncState.AggregatorSecondarySyncStateKeys;
import io.harness.aggregator.models.AggregatorSecondarySyncState.SecondarySyncStatus;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import java.util.Optional;
import javax.validation.constraints.NotNull;
import javax.validation.executable.ValidateOnExecution;
import org.hibernate.validator.constraints.NotEmpty;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

@OwnedBy(HarnessTeam.PL)
@Singleton
@ValidateOnExecution
public class AggregatorSecondarySyncStateRepository {
  private final MongoTemplate mongoTemplate;

  @Inject
  public AggregatorSecondarySyncStateRepository(@Named("mongoTemplate") MongoTemplate mongoTemplate) {
    this.mongoTemplate = mongoTemplate;
  }

  public AggregatorSecondarySyncState create(AggregatorSecondarySyncState aggregatorSecondarySyncState) {
    return mongoTemplate.save(aggregatorSecondarySyncState);
  }

  public Optional<AggregatorSecondarySyncState> findByIdentifier(@NotEmpty String identifier) {
    Criteria criteria = Criteria.where(AggregatorSecondarySyncStateKeys.identifier).is(identifier);
    return Optional.ofNullable(mongoTemplate.findOne(new Query(criteria), AggregatorSecondarySyncState.class));
  }

  public AggregatorSecondarySyncState updateStatus(
      @NotNull AggregatorSecondarySyncState obj, @NotNull SecondarySyncStatus status) {
    obj.setSecondarySyncStatus(status);
    return mongoTemplate.save(obj);
  }

  public void removeByIdentifier(@NotEmpty String identifier) {
    Criteria criteria = Criteria.where(AggregatorSecondarySyncStateKeys.identifier).is(identifier);
    mongoTemplate.findAllAndRemove(new Query(criteria), AggregatorSecondarySyncState.class);
  }
}
