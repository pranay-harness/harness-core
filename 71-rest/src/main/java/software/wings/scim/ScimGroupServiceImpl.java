package software.wings.scim;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.exception.WingsException.GROUP;

import com.google.inject.Inject;

import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnauthorizedException;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.service.intfc.UserGroupService;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Slf4j
public class ScimGroupServiceImpl implements ScimGroupService {
  @Inject private UserGroupService userGroupService;
  @Inject private WingsPersistence wingsPersistence;
  private static final String EXC_MSG_GROUP_DOESNT_EXIST = "Group does not exist";
  private static final String DISPLAY_NAME = "displayName";
  private static final Integer MAX_RESULT_COUNT = 20;
  private static final String REPLACE_OKTA = "replace";
  private static final String REPLACE = "Replace";
  private static final String ADD = "ADD";
  private static final String ADD_OKTA = "add";
  private static final String REMOVE = "REMOVE";
  private static final String REMOVE_OKTA = "remove";

  @Override
  public ScimListResponse<ScimGroup> searchGroup(String filter, String accountId, Integer count, Integer startIndex) {
    startIndex = startIndex == null ? 0 : startIndex;
    count = count == null ? MAX_RESULT_COUNT : count;
    ScimListResponse<ScimGroup> searchGroupResponse = new ScimListResponse<>();
    logger.info("SCIM: Searching groups in account {} with filter: {}", accountId, filter);
    String searchQuery = null;

    if (StringUtils.isNotEmpty(filter)) {
      try {
        filter = URLDecoder.decode(filter, "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        searchQuery = operand.substring(1, operand.length() - 1);
      } catch (Exception ex) {
        logger.error("SCIM: Failed to process group search query: {} for account: {}", filter, accountId, ex);
      }
    }

    List<ScimGroup> groupList = new ArrayList<>();

    try {
      groupList = searchUserGroupByName(accountId, searchQuery, count, startIndex);
      groupList.forEach(searchGroupResponse::resource);
    } catch (WingsException ex) {
      logger.info("SCIM: Search query: {}, account: {}", searchQuery, accountId, ex);
    }

    searchGroupResponse.startIndex(startIndex);
    searchGroupResponse.itemsPerPage(count);
    searchGroupResponse.totalResults(groupList.size());
    return searchGroupResponse;
  }

  private List<ScimGroup> searchUserGroupByName(
      String accountId, String searchQuery, Integer count, Integer startIndex) {
    Query<UserGroup> userGroupQuery =
        wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountId).equal(accountId);
    if (StringUtils.isNotEmpty(searchQuery)) {
      userGroupQuery.field(UserGroupKeys.name).equal(searchQuery);
    }

    List<UserGroup> userGroupList = userGroupQuery.asList(new FindOptions().skip(startIndex).limit(count));

    List<ScimGroup> scimGroupList = new ArrayList<>();
    for (UserGroup userGroup : userGroupList) {
      scimGroupList.add(buildGroupResponse(userGroup));
    }
    return scimGroupList;
  }

  private void updateDisplayName(
      ScimGroup scimGroup, UserGroup userGroup, UpdateOperations<UserGroup> updateOperations) {
    String newDisplayName = scimGroup.getDisplayName();
    if (StringUtils.isNotEmpty(newDisplayName) && !userGroup.getName().equals(newDisplayName)) {
      updateOperations.set(UserGroupKeys.name, newDisplayName);
    }
  }

  @Override
  public Response updateGroup(String groupId, String accountId, ScimGroup scimGroup) {
    logger.info(
        "SCIM: Update group call with groupId: {}, groupId {}, group resource:{}", groupId, accountId, scimGroup);
    UserGroup userGroup = userGroupService.get(accountId, groupId, false);
    if (userGroup == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    UpdateOperations<UserGroup> updateOperations = wingsPersistence.createUpdateOperations(UserGroup.class);
    updateDisplayName(scimGroup, userGroup, updateOperations);
    updateMembers(scimGroup, updateOperations);

    wingsPersistence.update(userGroup, updateOperations);
    logger.info("SCIM: Update group call successful groupId: {}, groupId {}, group resource: {}", groupId, accountId,
        scimGroup);
    return Response.status(Status.OK).entity(scimGroup).build();
  }

  private void updateMembers(ScimGroup scimGroup, UpdateOperations<UserGroup> updateOperations) {
    updateOperations.set(UserGroupKeys.memberIds, getMembersOfUserGroup(scimGroup));
  }

  @Override
  public void deleteGroup(String groupId, String accountId) {
    wingsPersistence.delete(accountId, UserGroup.class, groupId);
  }

  private String processReplaceOperationOnGroup(String groupId, String accountId, PatchOperation patchOperation) {
    if (!DISPLAY_NAME.equals(patchOperation.getPath())) {
      logger.error(
          "SCIM: Expected replace operation only on the displayName. Received it on path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      // no operation needed. Pass
    } else {
      try {
        return patchOperation.getValue(String.class);
      } catch (Exception ex) {
        logger.error("SCIM: Failed to process the operation: {}, for accountId: {}, for GroupId {}",
            patchOperation.toString(), accountId, groupId, ex);
      }
    }
    throw new InvalidRequestException("Failed to update group name");
  }

  private String processOktaReplaceOperationOnGroup(String groupId, String accountId, PatchOperation patchOperation) {
    try {
      if (patchOperation.getValue(ScimMultiValuedObject.class) != null) {
        return patchOperation.getValue(ScimMultiValuedObject.class).getDisplayName();
      }
    } catch (Exception ex) {
      logger.error("SCIM: Failed to process the REPLACE_OKTA operation: {}, for accountId: {}, for GroupId {}",
          patchOperation.toString(), accountId, groupId, ex);
    }
    throw new InvalidRequestException("Failed to update group name");
  }

  @Override
  public Response updateGroup(String groupId, String accountId, software.wings.scim.PatchRequest patchRequest) {
    UserGroup existingGroup = userGroupService.get(accountId, groupId);

    if (existingGroup == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    List<String> existingMemberIds =
        existingGroup.getMemberIds() != null ? existingGroup.getMemberIds() : new ArrayList<>();
    List<String> newMemberIds = new ArrayList<>(existingMemberIds);
    String newGroupName = null;

    for (software.wings.scim.PatchOperation patchOperation : patchRequest.getOperations()) {
      Set<String> userIdsFromOperation = getUserIdsFromOperation(patchOperation, accountId, groupId);
      switch (patchOperation.getOpType()) {
        case REPLACE: {
          newGroupName = processReplaceOperationOnGroup(groupId, accountId, patchOperation);
          break;
        }
        case REPLACE_OKTA: {
          newGroupName = processOktaReplaceOperationOnGroup(groupId, accountId, patchOperation);
          break;
        }
        case ADD:
        case ADD_OKTA: {
          updateNewMemberIds(userIdsFromOperation, newMemberIds);
          break;
        }
        case REMOVE:
        case REMOVE_OKTA: {
          newMemberIds.removeAll(userIdsFromOperation);
          break;
        }
        default: {
          logger.error("SCIM: Received unexpected PATCH operation: {}", patchOperation.toString());
          break;
        }
      }
    }

    UpdateOperations<UserGroup> updateOperations = wingsPersistence.createUpdateOperations(UserGroup.class);
    boolean updateGroup = false;
    if (StringUtils.isNotEmpty(newGroupName)) {
      updateOperations.set(UserGroupKeys.name, newGroupName);
      updateGroup = true;
    }
    if (!existingMemberIds.equals(newMemberIds)) {
      updateOperations.set(UserGroupKeys.memberIds, newMemberIds);
      updateGroup = true;
    }
    if (updateGroup) {
      wingsPersistence.update(existingGroup, updateOperations);
    }
    return Response.status(Status.NO_CONTENT).build();
  }

  private void updateNewMemberIds(Set<String> userIdsFromOperation, List<String> newMemberIds) {
    for (String userId : userIdsFromOperation) {
      User user = wingsPersistence.get(User.class, userId);
      if (user != null) {
        newMemberIds.add(userId);
      } else {
        logger.info("SCIM: No user exists in the db with the id {}", userId);
      }
    }
  }

  private Set<String> getUserIdsFromOperation(PatchOperation patchOperation, String accountId, String groupId) {
    if (patchOperation.getPath() != null && patchOperation.getPath().contains("members[")) {
      try {
        Set<String> userIds = new HashSet<>();
        String filter = URLDecoder.decode(patchOperation.getPath(), "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        userIds.add(operand.substring(1, operand.length() - 2));
        return userIds;
      } catch (Exception ex) {
        logger.error("SCIM: Not able to decode path. Received it in path: {}, for accountId: {}, for GroupId {}",
            patchOperation.getPath(), accountId, groupId, ex);
      }
    }

    if (!"members".equals(patchOperation.getPath())) {
      logger.error(
          "SCIM: Expect operation only on the members. Received it in path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      return Collections.emptySet();
    }

    try {
      List<? extends ScimMultiValuedObject> operations = patchOperation.getValues(ScimMultiValuedObject.class);
      if (!isEmpty(operations)) {
        return operations.stream().map(operation -> (String) operation.getValue()).collect(Collectors.toSet());
      }
      logger.error("SCIM: Operations received is null. Skipping remove operation processing for groupId: {}", groupId);
    } catch (Exception ex) {
      logger.error("SCIM: Failed to process the operation: {}, for accountId: {}, for GroupId {}",
          patchOperation.toString(), accountId, groupId, ex);
    }

    return Collections.emptySet();
  }

  @Override
  public ScimGroup getGroup(String groupId, String accountId) {
    UserGroup userGroup = userGroupService.get(accountId, groupId);
    if (userGroup == null) {
      throw new UnauthorizedException(EXC_MSG_GROUP_DOESNT_EXIST, GROUP);
    }
    ScimGroup scimGroup = buildGroupResponse(userGroup);
    logger.info("SCIM: Response to get group call: {}", scimGroup);
    return scimGroup;
  }

  private ScimGroup buildGroupResponse(UserGroup userGroup) {
    ScimGroup scimGroup = new ScimGroup();
    if (userGroup != null) {
      scimGroup.setId(userGroup.getUuid());
      scimGroup.setDisplayName(userGroup.getName());
      List<Member> memberList = new ArrayList<>();

      if (userGroup.getMembers() != null) {
        userGroup.getMembers().forEach(member -> {
          Member member1 = new Member();
          member1.setValue(member.getUuid());
          member1.setDisplay(member.getEmail());
          memberList.add(member1);
        });
      }

      scimGroup.setMembers(memberList);
    }
    return scimGroup;
  }

  @Override
  public ScimGroup createGroup(ScimGroup groupQuery, String accountId) {
    logger.info("SCIM: Creating group call: {}", groupQuery);

    UserGroup userGroup = UserGroup.builder()
                              .accountId(accountId)
                              .name(groupQuery.getDisplayName())
                              .memberIds(getMembersOfUserGroup(groupQuery))
                              .importedByScim(true)
                              .build();
    userGroupService.save(userGroup);

    groupQuery.setId(userGroup.getUuid());
    logger.info("SCIM: Completed creating group call: {}", groupQuery);
    return getGroup(userGroup.getUuid(), accountId);
  }

  private List<String> getMembersOfUserGroup(ScimGroup scimGroup) {
    List<String> newMemberIds = new ArrayList<>();
    scimGroup.getMembers().forEach(member -> {
      if (!newMemberIds.contains(member.getValue())) {
        User user = wingsPersistence.get(User.class, member.getValue());
        if (user != null) {
          newMemberIds.add(member.getValue());
        } else {
          logger.info("SCIM: No user exists in the db with the id {}", member.getValue());
        }
      }
    });
    return newMemberIds;
  }
}
