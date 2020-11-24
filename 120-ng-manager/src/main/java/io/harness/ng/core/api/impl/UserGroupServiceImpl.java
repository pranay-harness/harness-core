package io.harness.ng.core.api.impl;

import static io.harness.exception.WingsException.USER_SRE;
import static io.harness.ng.core.utils.NGUtils.verifyValuesNotChanged;
import static io.harness.ng.core.utils.UserGroupMapper.toEntity;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

import io.harness.exception.DuplicateFieldException;
import io.harness.ng.core.api.UserGroupService;
import io.harness.ng.core.dto.NotificationSettingType;
import io.harness.ng.core.dto.UserGroupDTO;
import io.harness.ng.core.entities.NotificationSettingConfig;
import io.harness.ng.core.entities.UserGroup;
import io.harness.ng.core.entities.UserGroup.UserGroupKeys;
import io.harness.repositories.ng.core.spring.UserGroupRepository;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

@Singleton
@Slf4j
public class UserGroupServiceImpl implements UserGroupService {
  private final UserGroupRepository userGroupRepository;

  @Inject
  public UserGroupServiceImpl(UserGroupRepository userGroupRepository) {
    this.userGroupRepository = userGroupRepository;
  }

  @Override
  public UserGroup create(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, UserGroupDTO userGroupDTO) {
    validateCreateUserGroupRequest(accountIdentifier, orgIdentifier, projectIdentifier, userGroupDTO);
    userGroupDTO.setAccountIdentifier(accountIdentifier);
    userGroupDTO.setOrgIdentifier(orgIdentifier);
    userGroupDTO.setProjectIdentifier(projectIdentifier);
    try {
      UserGroup userGroup = toEntity(userGroupDTO);
      Set<NotificationSettingType> typeSet = new HashSet<>();
      for (NotificationSettingConfig config : userGroup.getNotificationConfigs()) {
        if (typeSet.contains(config.getType())) {
          throw new IllegalArgumentException(
              "Not allowed to create multiple notification setting of type: " + config.getType());
        }
        typeSet.add(config.getType());
      }
      return userGroupRepository.save(userGroup);
    } catch (DuplicateKeyException ex) {
      throw new DuplicateFieldException(
          String.format("Try using different user group identifier, [%s] cannot be used", userGroupDTO.getIdentifier()),
          USER_SRE, ex);
    }
  }

  @Override
  public Page<UserGroup> list(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm, Pageable pageable) {
    return userGroupRepository.findAll(
        createUserGroupFilterCriteria(accountIdentifier, orgIdentifier, projectIdentifier, searchTerm), pageable);
  }

  @Override
  public Page<UserGroup> list(List<String> userGroupIds) {
    Criteria criteria = Criteria.where(UserGroupKeys.id).in(userGroupIds).and(UserGroupKeys.deleted).ne(Boolean.TRUE);
    return userGroupRepository.findAll(criteria, Pageable.unpaged());
  }

  private void validateCreateUserGroupRequest(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, UserGroupDTO userGroupDTO) {
    verifyValuesNotChanged(Lists.newArrayList(Pair.of(accountIdentifier, userGroupDTO.getAccountIdentifier()),
                               Pair.of(orgIdentifier, userGroupDTO.getOrgIdentifier()),
                               Pair.of(projectIdentifier, userGroupDTO.getProjectIdentifier())),
        true);
  }

  private Criteria createUserGroupFilterCriteria(
      String accountIdentifier, String orgIdentifier, String projectIdentifier, String searchTerm) {
    Criteria criteria = Criteria.where(UserGroupKeys.accountIdentifier)
                            .is(accountIdentifier)
                            .and(UserGroupKeys.orgIdentifier)
                            .is(orgIdentifier)
                            .and(UserGroupKeys.projectIdentifier)
                            .is(projectIdentifier)
                            .and(UserGroupKeys.deleted)
                            .ne(Boolean.TRUE);
    if (isNotBlank(searchTerm)) {
      criteria.orOperator(Criteria.where(UserGroupKeys.name).regex(searchTerm, "i"),
          Criteria.where(UserGroupKeys.tags).regex(searchTerm, "i"));
    }
    return criteria;
  }
}
