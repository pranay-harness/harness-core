package software.wings.scim;

import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import com.google.common.collect.Lists;
import com.google.inject.Inject;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.harness.eraro.ErrorCode;
import io.harness.exception.WingsException;
import io.harness.serializer.JsonUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.User;
import software.wings.beans.User.UserKeys;
import software.wings.beans.UserInvite;
import software.wings.beans.UserInvite.UserInviteBuilder;
import software.wings.beans.scim.ScimUser;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserService;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Slf4j
public class ScimUserServiceImpl implements ScimUserService {
  @Inject private UserService userService;
  @Inject private WingsPersistence wingsPersistence;

  private Integer MAX_RESULT_COUNT = 20;

  @Override
  public Response createUser(ScimUser userQuery, String accountId) {
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
        return Response.status(Status.CREATED).entity(getUser(user.getUuid(), accountId)).build();
      } else {
        return Response.status(Status.CONFLICT).entity(getUser(user.getUuid(), accountId)).build();
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

    if (user != null) {
      userQuery.setId(user.getUuid());
      logger.info("SCIM: Completed creating user call: {}", userQuery);
      return Response.status(Status.CREATED).entity(getUser(user.getUuid(), accountId)).build();
    } else {
      return Response.status(Status.NOT_FOUND).build();
    }
  }

  private String getPrimaryEmail(ScimUser userQuery) {
    // Another alternate way is to get it from the list of emails.work.
    return userQuery.getUserName();
  }

  @Override
  public ScimUser getUser(String userId, String accountId) {
    User user = userService.get(accountId, userId);
    return buildUserResponse(user);
  }

  private ScimUser buildUserResponse(User user) {
    ScimUser userResource = new ScimUser();

    if (user == null) {
      return null;
    }

    String userName = user.getName();

    userResource.setId(user.getUuid());
    userResource.setUserName(user.getEmail());
    userResource.setActive(true);
    userResource.setUserName(user.getEmail());
    userResource.setDisplayName(userName);

    Map<String, String> nameMap = new HashMap<String, String>() {
      {
        put("givenName", user.getName());
        put("familyName", user.getName());
      }
    };

    userResource.setActive(true);
    Map<String, String> emailMap = new HashMap<String, String>() {
      { put("value", user.getEmail()); }
    };
    userResource.setEmails(JsonUtils.asTree(Collections.singletonList(emailMap)));
    userResource.setName(JsonUtils.asTree(nameMap));
    return userResource;
  }

  @Override
  public ScimListResponse<ScimUser> searchUser(String accountId, String filter, Integer count, Integer startIndex) {
    startIndex = startIndex == null ? 0 : startIndex;
    count = count == null ? MAX_RESULT_COUNT : count;
    logger.info("Searching users in account {} with filter: {}", accountId, filter);

    ScimListResponse<ScimUser> userResponse = new ScimListResponse<>();
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

    List<ScimUser> scimUsers = new ArrayList<>();
    try {
      scimUsers = searchUserByUserName(accountId, searchQuery, count, startIndex);
      scimUsers.forEach(userResponse::resource);
    } catch (WingsException ex) {
      logger.info("Search user by name failed. searchQuery: {}, account: {}", searchQuery, accountId, ex);
    }

    userResponse.startIndex(startIndex);
    userResponse.itemsPerPage(count);
    userResponse.totalResults(scimUsers.size());
    return userResponse;
  }

  private List<ScimUser> searchUserByUserName(String accountId, String searchQuery, Integer count, Integer startIndex) {
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
  public ScimUser updateUser(String accountId, String userId, PatchRequest patchRequest) {
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
      throws JsonProcessingException {
    User user = userService.get(accountId, userId);

    if (user == null) {
      throw new WingsException(ErrorCode.USER_DOES_NOT_EXIST);
    }

    if ("displayName".equals(patchOperation.getPath())) {
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
  public Response updateUser(String userId, String accountId, ScimUser userResource) {
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
