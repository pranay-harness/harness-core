/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.graphql.datafetcher.user;

import static io.harness.persistence.HQuery.excludeAuthority;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.TargetModule;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;
import io.harness.persistence.HIterator;
import io.harness.persistence.HPersistence;

import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.graphql.datafetcher.AbstractObjectDataFetcher;
import software.wings.graphql.schema.query.QLUserQueryParameters;
import software.wings.graphql.schema.type.QLUser;
import software.wings.graphql.schema.type.QLUser.QLUserBuilder;
import software.wings.security.PermissionAttribute;
import software.wings.security.annotations.AuthRule;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.mongodb.morphia.query.CriteriaContainer;
import org.mongodb.morphia.query.Query;

@Slf4j
@TargetModule(HarnessModule._380_CG_GRAPHQL)
public class UserDataFetcher extends AbstractObjectDataFetcher<QLUser, QLUserQueryParameters> {
  public static final String USER_DOES_NOT_EXIST_MSG = "User does not exist";
  @Inject HPersistence persistence;

  @Override
  @AuthRule(permissionType = PermissionAttribute.PermissionType.USER_PERMISSION_READ)
  public QLUser fetch(QLUserQueryParameters qlQuery, String accountId) {
    User user = null;
    if (qlQuery.getId() != null) {
      user =
          persistence.createQuery(User.class).filter("_id", qlQuery.getId()).filter(UserKeys.accounts, accountId).get();
    }
    if (qlQuery.getName() != null) {
      try (HIterator<User> iterator = new HIterator<>(persistence.createQuery(User.class)
                                                          .filter(UserKeys.name, qlQuery.getName())
                                                          .filter(UserKeys.accounts, accountId)
                                                          .fetch())) {
        if (iterator.hasNext()) {
          user = iterator.next();
        }
      }
    }
    if (qlQuery.getEmail() != null) {
      try (HIterator<User> iterator = new HIterator<>(getSearchByEmailQuery(accountId, qlQuery.getEmail()).fetch())) {
        if (iterator.hasNext()) {
          user = iterator.next();
        }
      }
    }
    if (user == null) {
      throw new InvalidRequestException(USER_DOES_NOT_EXIST_MSG, WingsException.USER);
    }
    final QLUserBuilder builder = QLUser.builder();
    UserController.populateUser(user, builder);
    return builder.build();
  }

  private Query<User> getSearchByEmailQuery(String accountId, String email) {
    Query<User> query = persistence.createQuery(User.class, excludeAuthority);
    CriteriaContainer inviteAccepted =
        query.and(query.criteria(UserKeys.email).equal(email), query.criteria(UserKeys.accounts).hasThisOne(accountId));
    CriteriaContainer invitePending = query.and(
        query.criteria(UserKeys.email).equal(email), query.criteria(UserKeys.pendingAccounts).hasThisOne(accountId));
    query.or(inviteAccepted, invitePending);
    return query;
  }
}
