package io.harness.core.userchangestream;

import static io.harness.mongo.changestreams.ChangeType.DELETE;
import static io.harness.mongo.changestreams.ChangeType.INSERT;
import static io.harness.mongo.changestreams.ChangeType.REPLACE;
import static io.harness.mongo.changestreams.ChangeType.UPDATE;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import io.harness.core.userchangestream.UserMembershipChangeStreamState.UserEntityChangeStreamStateKeys;
import io.harness.eventsframework.EventsFrameworkConstants;
import io.harness.eventsframework.api.Producer;
import io.harness.eventsframework.producer.Message;
import io.harness.eventsframework.schemas.useraccountmembership.UserAccountMembershipDTO;
import io.harness.mongo.changestreams.ChangeEvent;
import io.harness.mongo.changestreams.ChangeType;

import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.dl.WingsPersistence;

import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@Slf4j
public class UserMembershipChangeStreamProcessor {
  @Inject @Named(EventsFrameworkConstants.USER_ACCOUNT_MEMBERSHIP) Producer eventProducer;
  @Inject WingsPersistence wingsPersistence;

  boolean processChangeEvent(ChangeEvent<?> changeEvent) {
    log.info("Received change event with token {}", changeEvent.getToken());
    assert User.class.isAssignableFrom(changeEvent.getEntityType());
    boolean success = process(changeEvent);

    if (success) {
      success = updateLastSuccessfullyProcessedToken(changeEvent);
    }
    return success;
  }

  private boolean updateLastSuccessfullyProcessedToken(ChangeEvent<?> changeEvent) {
    String token = changeEvent.getToken();
    Query<UserMembershipChangeStreamState> query = wingsPersistence.createQuery(UserMembershipChangeStreamState.class)
                                                       .field(UserEntityChangeStreamStateKeys.id)
                                                       .equal(UserMembershipChangeStreamState.ID_VALUE);

    UpdateOperations<UserMembershipChangeStreamState> updateOperations =
        wingsPersistence.createUpdateOperations(UserMembershipChangeStreamState.class)
            .set(UserEntityChangeStreamStateKeys.lastSyncedToken, token);

    UserMembershipChangeStreamState userMembershipChangeStreamState =
        wingsPersistence.upsert(query, updateOperations, upsertReturnNewOptions);
    if (userMembershipChangeStreamState == null
        || !userMembershipChangeStreamState.getLastSyncedToken().equals(token)) {
      log.error("User-Account membership change stream state couldn't be updated to {}", token);
      return false;
    }
    return true;
  }

  private boolean process(ChangeEvent<?> changeEvent) {
    UserAccountMembershipDTO userAccountMembershipDTO = getUserAccountMembership(changeEvent);
    if (userAccountMembershipDTO != null) {
      try {
        eventProducer.send(Message.newBuilder().setData(userAccountMembershipDTO.toByteString()).build());
      } catch (Exception ex) {
        log.error("Error while publishing event for user {}", changeEvent.getUuid());
        return false;
      }
    }
    return true;
  }

  private UserAccountMembershipDTO getUserAccountMembership(ChangeEvent<?> changeEvent) {
    ChangeType changeType = changeEvent.getChangeType();
    boolean isUpdateEvent = isUpdateEvent(changeEvent);
    String userId = changeEvent.getUuid();
    if (isUpdateEvent || changeType.equals(INSERT) || changeType.equals(REPLACE)) {
      User user = (User) changeEvent.getFullDocument();
      return UserAccountMembershipDTO.newBuilder()
          .setAction(changeType.toString())
          .setUserId(user.getUuid())
          .setUserEmail(user.getEmail())
          .addAllAccounts(user.getAccountIds())
          .build();
    } else if (changeType.equals(DELETE)) {
      return UserAccountMembershipDTO.newBuilder().setAction(changeType.toString()).setUserId(userId).build();
    }
    return null;
  }

  private boolean isUpdateEvent(ChangeEvent<?> changeEvent) {
    return changeEvent.getChangeType().equals(UPDATE)
        && changeEvent.getChanges().keySet().stream().anyMatch(e -> e.split("\\.")[0].equals(UserKeys.accounts));
  }
}
