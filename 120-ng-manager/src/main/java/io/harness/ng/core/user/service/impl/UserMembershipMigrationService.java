package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofSeconds;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.iterator.PersistenceIteratorFactory;
import io.harness.mongo.iterator.MongoPersistenceIterator;
import io.harness.mongo.iterator.filter.SpringFilterExpander;
import io.harness.mongo.iterator.provider.SpringPersistenceProvider;
import io.harness.ng.core.user.UserInfo;
import io.harness.ng.core.user.entities.UserMembership;
import io.harness.ng.core.user.entities.UserMembership.UserMembershipKeys;
import io.harness.ng.core.user.service.NgUserService;
import io.harness.resourcegroup.model.ResourceGroup;

import com.google.inject.Inject;
import java.util.Optional;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Update;

@AllArgsConstructor(onConstructor = @__({ @Inject }))
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
@OwnedBy(PL)
public class UserMembershipMigrationService implements MongoPersistenceIterator.Handler<UserMembership> {
  @Inject PersistenceIteratorFactory persistenceIteratorFactory;
  @Inject MongoTemplate mongoTemplate;
  @Inject NgUserService ngUserService;

  @Override
  public void handle(UserMembership userMembership) {
    if (isNotBlank(userMembership.getName())) {
      return;
    }
    try {
      Optional<UserInfo> userOpt = ngUserService.getUserById(userMembership.getUserId());
      if (!userOpt.isPresent()) {
        return;
      }
      UserInfo user = userOpt.get();
      Update update = new Update().set(UserMembershipKeys.name, user.getName());
      ngUserService.update(userMembership.getUserId(), update);
    } catch (InvalidRequestException | UnexpectedException e) {
      /**
       * Do nothing. This exception will occur for users are present in nextgen but not registered in currentgen. This
       * will happen for stale users. It is to be decided whether to clean them up in nextgen or not
       */
    }
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(3)
            .interval(ofSeconds(1))
            .build(),
        ResourceGroup.class,
        MongoPersistenceIterator.<UserMembership, SpringFilterExpander>builder()
            .clazz(UserMembership.class)
            .fieldName(UserMembershipKeys.nextIteration)
            .targetInterval(ofSeconds(30))
            .acceptableNoAlertDelay(ofHours(1))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }
}