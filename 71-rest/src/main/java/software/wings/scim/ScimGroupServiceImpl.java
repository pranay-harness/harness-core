package software.wings.scim;

import com.google.inject.Inject;

import com.unboundid.scim2.client.requests.ListResponseBuilder;
import com.unboundid.scim2.common.messages.ListResponse;
import com.unboundid.scim2.common.messages.PatchOpType;
import io.harness.exception.WingsException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.beans.security.UserGroup;
import software.wings.beans.security.UserGroup.UserGroupKeys;
import software.wings.dl.WingsPersistence;
import software.wings.resources.ScimMultiValuedObject;
import software.wings.service.intfc.UserGroupService;

import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

@Slf4j
public class ScimGroupServiceImpl implements ScimGroupService {
  @Inject private UserGroupService userGroupService;
  @Inject private WingsPersistence wingsPersistence;

  private Integer MAX_RESULT_COUNT = 20;

  @Override
  public ListResponse<GroupResource> searchGroup(String filter, String accountId, Integer count, Integer startIndex) {
    startIndex = startIndex == null ? 1 : startIndex;
    count = count == null ? MAX_RESULT_COUNT : count;
    ListResponseBuilder<GroupResource> groupResourceListResponseBuilder = new ListResponseBuilder<>();
    logger.info("Searching groups in account {} with filter: {}", accountId, filter);
    String searchQuery = null;

    if (StringUtils.isNotEmpty(filter)) {
      try {
        filter = URLDecoder.decode(filter, "UTF-8");
        String[] split = filter.split(" eq ");
        String operand = split[1];
        searchQuery = operand.substring(1, operand.length() - 1);
      } catch (Exception ex) {
        logger.error("SCIM: Failed to process group search query: {} for account: {}", filter, accountId);
      }
    }

    List<GroupResource> groupResourceList = new ArrayList<>();

    try {
      groupResourceList = searchUserGroupByName(accountId, searchQuery, count, startIndex);
      groupResourceList.forEach(groupResourceListResponseBuilder::resource);
    } catch (WingsException ex) {
      logger.info("Search query: {}, account: {}", searchQuery, accountId, ex);
    }

    groupResourceListResponseBuilder.startIndex(startIndex);
    groupResourceListResponseBuilder.itemsPerPage(count);
    groupResourceListResponseBuilder.totalResults(groupResourceList.size());
    return groupResourceListResponseBuilder.build();
  }

  private List<GroupResource> searchUserGroupByName(
      String accountId, String searchQuery, Integer count, Integer startIndex) {
    Query<UserGroup> userGroupQuery =
        wingsPersistence.createQuery(UserGroup.class).field(UserGroupKeys.accountId).equal(accountId);
    if (StringUtils.isNotEmpty(searchQuery)) {
      userGroupQuery.field(UserGroupKeys.name).equal(searchQuery);
    }

    List<UserGroup> userGroupList = userGroupQuery.asList(new FindOptions().skip(startIndex).limit(count));

    List<GroupResource> groupResourceList = new ArrayList<>();
    for (UserGroup userGroup : userGroupList) {
      groupResourceList.add(buildGroupResponse(userGroup));
    }
    return groupResourceList;
  }

  private void updateDisplayName(
      GroupResource groupResource, UserGroup userGroup, UpdateOperations<UserGroup> updateOperations) {
    String newDisplayName = groupResource.getDisplayName();
    if (StringUtils.isNotEmpty(newDisplayName) && !userGroup.getName().equals(newDisplayName)) {
      updateOperations.set(UserGroupKeys.name, newDisplayName);
    }
  }

  @Override
  public Response updateGroup(String groupId, String accountId, GroupResource groupResource) {
    logger.info("Update group call with groupId: {}, groupId {}, group resource:{}", groupId, accountId, groupResource);
    UserGroup userGroup = userGroupService.get(accountId, groupId, false);
    if (userGroup == null) {
      return Response.status(Status.NOT_FOUND).build();
    }

    UpdateOperations<UserGroup> updateOperations = wingsPersistence.createUpdateOperations(UserGroup.class);
    updateDisplayName(groupResource, userGroup, updateOperations);
    updateMembers(groupResource, updateOperations);

    wingsPersistence.update(userGroup, updateOperations);
    logger.info(
        "Update group call successful groupId: {}, groupId {}, group resource: {}", groupId, accountId, groupResource);
    return Response.status(Status.OK).entity(groupResource).build();
  }

  private void updateMembers(GroupResource groupResource, UpdateOperations<UserGroup> updateOperations) {
    updateOperations.set(UserGroupKeys.memberIds, getMembersOfUserGroup(groupResource));
  }

  @Override
  public void deleteGroup(String groupId, String accountId) {
    wingsPersistence.delete(accountId, UserGroup.class, groupId);
  }

  private String processReplaceOperationOnGroup(String groupId, String accountId, PatchOperation patchOperation) {
    if (!"displayName".equals(patchOperation.getPath().toString())) {
      logger.error(
          "Expected replace operation only on the displayName. Received it on path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      // no operation needed. Pass
    } else {
      try {
        return patchOperation.getValue(String.class);
      } catch (Exception ex) {
        logger.error("Failed to process the operation: {}, for accountId: {}, for GroupId {}",
            patchOperation.toString(), accountId, groupId);
      }
    }
    throw new WingsException("Failed to update group name");
  }

  private void processAddOperationOnGroup(
      String groupId, String accountId, List<String> memberIds, PatchOperation patchOperation) {
    if (!"members".equals(patchOperation.getPath().toString())) {
      logger.error(
          "Expect add operation only on the members. Received it on path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      // no operation needed. Pass
    } else {
      try {
        List<? extends ScimMultiValuedObject> operations =
            patchOperation.getValues(new ScimMultiValuedObject<List<String>>().getClass());

        for (ScimMultiValuedObject operation : operations) {
          String userId = (String) operation.getValue();
          if (!memberIds.contains(userId)) {
            memberIds.add(userId);
          }
        }
      } catch (Exception ex) {
        logger.error("Failed to process the operation: {}, for accountId: {}, for GroupId {}",
            patchOperation.toString(), accountId, groupId);
      }
    }
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
    String newGroupName = "";

    for (software.wings.scim.PatchOperation patchOperation : patchRequest.getOperations()) {
      if (patchOperation.getOpType().equals(PatchOpType.REPLACE)) {
        newGroupName = processReplaceOperationOnGroup(groupId, accountId, patchOperation);
      } else if (patchOperation.getOpType().equals(PatchOpType.ADD)) {
        processAddOperationOnGroup(groupId, accountId, newMemberIds, patchOperation);
      } else if (patchOperation.getOpType().equals(PatchOpType.REMOVE)) {
        processRemoveOperationOnGroup(groupId, accountId, newMemberIds, patchOperation);
      } else {
        logger.error("Received unexpected operation: {}", patchOperation.toString());
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

  private void processRemoveOperationOnGroup(
      String groupId, String accountId, List<String> memberIds, PatchOperation patchOperation) {
    if (!"members".equals(patchOperation.getPath().toString())) {
      logger.error(
          "Expect remove operation only on the members. Received it on path: {}, for accountId: {}, for GroupId {}",
          patchOperation.getPath(), accountId, groupId);
      // np operation needed. Pass
    } else {
      try {
        List<? extends ScimMultiValuedObject> operations =
            patchOperation.getValues(new ScimMultiValuedObject<List<String>>().getClass());

        for (ScimMultiValuedObject operation : operations) {
          String userId = (String) operation.getValue();
          if (memberIds.contains(userId)) {
            memberIds.remove(userId);
          }
        }
      } catch (Exception ex) {
        logger.error("Failed to process the operation: {}, for accountId: {}, for GroupId {}",
            patchOperation.toString(), accountId, groupId);
      }
    }
  }

  @Override
  public GroupResource getGroup(String groupId, String accountId) {
    UserGroup userGroup = userGroupService.get(accountId, groupId);
    GroupResource groupResource = buildGroupResponse(userGroup);
    logger.info("Response to get group call: {}", groupResource);
    return groupResource;
  }

  private GroupResource buildGroupResponse(UserGroup userGroup) {
    GroupResource groupResource = new GroupResource();
    if (userGroup != null) {
      groupResource.setId(userGroup.getUuid());
      groupResource.setDisplayName(userGroup.getName());
      List<Member> memberList = new ArrayList<>();

      if (userGroup.getMembers() != null) {
        userGroup.getMembers().forEach(member -> {
          Member member1 = new Member();
          member1.setValue(member.getUuid());
          member1.setDisplay(member.getEmail());
          memberList.add(member1);
        });
      }

      groupResource.setMembers(memberList);
    }
    return groupResource;
  }

  @Override
  public GroupResource createGroup(GroupResource groupQuery, String accountId) {
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
    return groupQuery;
  }

  private List<String> getMembersOfUserGroup(GroupResource groupResource) {
    List<String> newMemberIds = new ArrayList<>();
    groupResource.getMembers().forEach(member -> {
      if (!newMemberIds.contains(member.getValue())) {
        newMemberIds.add(member.getValue());
      }
    });
    return newMemberIds;
  }
}
