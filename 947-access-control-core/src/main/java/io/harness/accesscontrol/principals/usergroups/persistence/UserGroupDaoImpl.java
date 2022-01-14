package io.harness.accesscontrol.principals.usergroups.persistence;

import static io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBOMapper.fromDBO;
import static io.harness.accesscontrol.principals.usergroups.persistence.UserGroupDBOMapper.toDBO;
import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.accesscontrol.principals.usergroups.UserGroup;
import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;
import io.harness.utils.PageUtils;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@OwnedBy(PL)
@Singleton
@ValidateOnExecution
public class UserGroupDaoImpl implements UserGroupDao {
  private final UserGroupRepository userGroupRepository;

  @Inject
  public UserGroupDaoImpl(UserGroupRepository userGroupRepository) {
    this.userGroupRepository = userGroupRepository;
  }

  @Override
  public UserGroup upsert(UserGroup userGroupUpdate) {
    UserGroupDBO userGroupDBOUpdate = toDBO(userGroupUpdate);
    Optional<UserGroupDBO> userGroupDBOOpt = userGroupRepository.findByIdentifierAndScopeIdentifier(
        userGroupDBOUpdate.getIdentifier(), userGroupDBOUpdate.getScopeIdentifier());
    if (userGroupDBOOpt.isPresent()) {
      UserGroupDBO userGroupDBO = userGroupDBOOpt.get();
      if (userGroupDBO.equals(userGroupDBOUpdate)) {
        return fromDBO(userGroupDBO);
      }
      userGroupDBOUpdate.setId(userGroupDBO.getId());
      userGroupDBOUpdate.setVersion(userGroupDBO.getVersion());
      userGroupDBOUpdate.setCreatedAt(userGroupDBO.getCreatedAt());
      userGroupDBOUpdate.setLastModifiedAt(userGroupDBO.getCreatedAt());
      userGroupDBOUpdate.setNextReconciliationIterationAt(userGroupDBO.getNextReconciliationIterationAt());
    }
    return fromDBO(userGroupRepository.save(userGroupDBOUpdate));
  }

  @Override
  public PageResponse<UserGroup> list(PageRequest pageRequest, String scopeIdentifier) {
    Pageable pageable = PageUtils.getPageRequest(pageRequest);
    Page<UserGroupDBO> userGroupPages = userGroupRepository.findByScopeIdentifier(scopeIdentifier, pageable);
    return PageUtils.getNGPageResponse(userGroupPages.map(UserGroupDBOMapper::fromDBO));
  }

  @Override
  public Optional<UserGroup> get(String identifier, String scopeIdentifier) {
    return userGroupRepository.findByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .flatMap(ug -> Optional.of(UserGroupDBOMapper.fromDBO(ug)));
  }

  @Override
  public Optional<UserGroup> delete(String identifier, String scopeIdentifier) {
    return userGroupRepository.deleteByIdentifierAndScopeIdentifier(identifier, scopeIdentifier)
        .stream()
        .findFirst()
        .flatMap(ug -> Optional.of(UserGroupDBOMapper.fromDBO(ug)));
  }
}
