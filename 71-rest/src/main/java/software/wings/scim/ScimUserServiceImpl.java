package software.wings.scim;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.unboundid.scim2.client.requests.ListResponseBuilder;
import com.unboundid.scim2.common.exceptions.ScimException;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.types.Email;
import com.unboundid.scim2.common.types.Name;
import com.unboundid.scim2.common.types.UserResource;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserService;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Slf4j
public class ScimUserServiceImpl implements ScimUserService {
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;

  private Integer MAX_RESULT_COUNT = 20;

  @Override
  public Response createUser(UserResource userQuery, String accountId) {
    logger.info("SCIM: Creating user call: {}", userQuery);
    User user = null;
    String primaryEmail = getPrimaryEmail(userQuery).toLowerCase();

    user = userService.getUserByEmail(primaryEmail, accountId);

    if (user != null) {
      userQuery.setId(user.getUuid());
      userQuery.setActive(true);
      // if the user already exists with with that email and is disabled, activate him.
      if (user.isDisabled()) {
        updateUser(user.getUuid(), accountId, userQuery);
        return Response.status(Status.CREATED).entity(userQuery).build();
      } else {
        return Response.status(Status.CONFLICT).entity(userQuery).build();
      }
    }

    UserInvite userInvite = UserInviteBuilder.anUserInvite()
                                .withAccountId(accountId)
                                .withEmail(primaryEmail)
                                .withName(userQuery.getDisplayName())
                                .withUserGroups(Lists.newArrayList())
                                .withImportedByScim(true)
                                .build();
    userService.inviteUser(userInvite);

    user = userService.getUserByEmail(primaryEmail, accountId);
    userQuery.setId(user.getUuid());
    logger.info("SCIM: Completed creating user call: {}", userQuery);
    return Response.status(Status.CREATED).entity(userQuery).build();
  }

  private String getPrimaryEmail(UserResource userQuery) {
    // Another alternate way is to get it from the list of emails.work.
    return userQuery.getUserName();
  }

  @Override
  public UserResource getUser(String userId, String accountId) {
    User user = userService.get(accountId, userId);
    return buildUserResponse(user);
  }

  private UserResource buildUserResponse(User user) {
    Name name = new Name();
    UserResource userResource = new UserResource();

    if (user == null) {
      return null;
    }

    String userName = user.getName();

    String[] nameSplit = userName.split(" ", 2);
    String firstName = nameSplit[0];
    String lastName = nameSplit.length > 1 ? nameSplit[1] : firstName;

    name.setGivenName(firstName);
    name.setFamilyName(lastName);

    Email email = new Email();
    email.setPrimary(true);
    email.setValue(user.getEmail());
    email.setType("work");

    name.setFormatted("givenName familyName");
    userResource.setName(name);
    userResource.setId(user.getUuid());
    userResource.setUserName(user.getEmail());
    userResource.setActive(true);
    userResource.setUserName(user.getEmail());
    userResource.setDisplayName(userName);
    userResource.setEmails(Arrays.asList(email));
    userResource.setActive(true);
    return userResource;
  }

  @Override
  public ListResponse<UserResource> searchUser(String accountId, String filter, Integer count, Integer startIndex) {
    startIndex = startIndex == null ? 0 : startIndex;
    count = count == null ? MAX_RESULT_COUNT : count;
    logger.info("Searching users in account {} with filter: {}", accountId, filter);

    ListResponseBuilder<UserResource> userResourceListResponseBuilder = new ListResponseBuilder<>();
    String searchQuery = null;
    if (isNotEmpty(filter)) {
      try {
        filter = URLDecoder.decode(filter, "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        searchQuery = operand.substring(1, operand.length() - 1);
      } catch (Exception ex) {
        logger.error("SCIM: Failed to process filter query: {} for account: {}", filter, accountId);
      }
    }

    List<UserResource> userResources = new ArrayList<>();
    try {
      userResources = searchUserByUserName(accountId, searchQuery, count, startIndex);
      userResources.forEach(userResourceListResponseBuilder::resource);
    } catch (WingsException ex) {
      logger.info("Search user by name failed. searchQuery: {}, account: {}", searchQuery, accountId, ex);
    }

    userResourceListResponseBuilder.startIndex(startIndex);
    userResourceListResponseBuilder.itemsPerPage(count);
    userResourceListResponseBuilder.totalResults(userResources.size());
    return userResourceListResponseBuilder.build();
  }

  private List<UserResource> searchUserByUserName(
      String accountId, String searchQuery, Integer count, Integer startIndex) {
    Query<User> userQuery = wingsPersistence.createQuery(User.class).field(UserKeys.accounts).hasThisOne(accountId);
    if (StringUtils.isNotEmpty(searchQuery)) {
      userQuery.field(UserKeys.email).equal(searchQuery);
    }

    List<User> userList = userQuery.asList(new FindOptions().skip(startIndex).limit(count));

    return userList.stream().map(this ::buildUserResponse).collect(Collectors.toList());
  }

  @Override
  public void deleteUser(String userId, String accountId) {
    logger.info("SCIM: deleting the user {} for accountId {}", userId, accountId);
    userService.delete(accountId, userId);
    logger.info("SCIM: deleting the user completed {} for accountId {}", userId, accountId);
  }

  @Override
  public UserResource updateUser(String accountId, String userId, PatchRequest patchRequest) {
    patchRequest.getOperations().forEach(patchOperation -> {
      try {
        applyUserUpdateOperation(accountId, userId, patchOperation);
      } catch (Exception ex) {
        logger.error("Failed to update user: {}, patchOperation: {}", userId, patchOperation, ex);
      }
    });
    return getUser(userId, accountId);
  }

  private void applyUserUpdateOperation(String accountId, String userId, PatchOperation patchOperation)
      throws ScimException, JsonProcessingException {
    User user = userService.get(accountId, userId);

    if (user == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }

    if ("displayName".equals(patchOperation.getPath().toString())) {
      UpdateOperations<User> updateOperation = wingsPersistence.createUpdateOperations(User.class);
      updateOperation.set(UserKeys.name, patchOperation.getValue(String.class));
      wingsPersistence.update(user, updateOperation);
    } else {
      // Not supporting any other updates as of now.
      logger.error("SCIM: Unexpected patch operation received: accountId: {}, userId: {}, patchOperation: {}",
          accountId, userId, patchOperation);
    }
  }

  @Override
  public Response updateUser(String userId, String accountId, UserResource userResource) {
    logger.info("Updating user resource: {}", userResource);
    User user = userService.get(accountId, userId);

    if (user == null) {
      return Response.status(Status.NOT_FOUND).build();
    } else {
      String displayName = userResource.getDisplayName();

      UpdateOperations<User> updateOperations = wingsPersistence.createUpdateOperations(User.class);

      boolean userUpdate = false;
      if (StringUtils.isNotEmpty(displayName) && !user.getName().equals(displayName)) {
        userUpdate = true;
        updateOperations.set(UserKeys.name, displayName);
        logger.info("Updated user's {} name: {}", userId, displayName);
      }

      boolean userEnabled = !user.isDisabled();

      if (userResource.getActive() != null && userResource.getActive() != userEnabled) {
        userUpdate = true;
        logger.info("Updated user's {}, enabled: {}", userId, userResource.getActive());
        updateOperations.set(UserKeys.disabled, !userResource.getActive());
      }
      if (userUpdate) {
        wingsPersistence.update(user, updateOperations);
      }
      return Response.status(Status.ACCEPTED).entity(userResource).build();
    }
  }
}
