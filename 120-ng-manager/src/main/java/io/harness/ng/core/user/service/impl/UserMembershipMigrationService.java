package io.harness.ng.core.user.service.impl;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.mongo.iterator.MongoPersistenceIterator.SchedulingType.REGULAR;

import static java.time.Duration.ofHours;
import static java.time.Duration.ofMinutes;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
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
    /**
     * Have to get the latest UserMemberhsip from the db other wise optimistic locking fails as mongo iterator framework
     * has already updated the collection while it was updating nextIteration. Other solution would be to just increment
     * the version in the UserMembership. But that is hacky
     */
    Optional<UserMembership> userMembershipOpt = ngUserService.getUserMembership(userMembership.getUserId());
    if (!userMembershipOpt.isPresent()) {
      return;
    }
    userMembership = userMembershipOpt.get();
    try {
      Optional<UserInfo> userOpt = ngUserService.getUserById(userMembership.getUserId());
      if (!userOpt.isPresent()) {
        return;
      }
      UserInfo user = userOpt.get();
      userMembership.setName(user.getName());
    } catch (InvalidRequestException e) {
      /**
       * Do nothing. This exception will occur for users are present in nextgen but not registered in currentgen. This
       * will happen for stale users. It is to be decided whether to clean them up in nextgen or not
       */
    }
    ngUserService.update(userMembership);
  }

  public void registerIterators() {
    persistenceIteratorFactory.createPumpIteratorWithDedicatedThreadPool(
        PersistenceIteratorFactory.PumpExecutorOptions.builder()
            .name(this.getClass().getName())
            .poolSize(3)
            .interval(ofMinutes(1))
            .build(),
        ResourceGroup.class,
        MongoPersistenceIterator.<UserMembership, SpringFilterExpander>builder()
            .clazz(UserMembership.class)
            .fieldName(UserMembershipKeys.nextIteration)
            .targetInterval(ofMinutes(30))
            .acceptableNoAlertDelay(ofHours(1))
            .handler(this)
            .schedulingType(REGULAR)
            .persistenceProvider(new SpringPersistenceProvider<>(mongoTemplate))
            .redistribute(true));
  }
}