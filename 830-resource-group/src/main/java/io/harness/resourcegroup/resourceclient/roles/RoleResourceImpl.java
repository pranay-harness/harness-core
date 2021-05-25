package io.harness.resourcegroup.resourceclient.roles;

import static io.harness.annotations.dev.HarnessTeam.PL;
import static io.harness.resourcegroup.beans.ValidatorType.DYNAMIC;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Inject;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@OwnedBy(PL)
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@AllArgsConstructor(access = AccessLevel.PUBLIC, onConstructor = @__({ @Inject }))
@Slf4j
public class RoleResourceImpl implements Resource {
  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    return Collections.emptyList();
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(DYNAMIC);
  }

  @Override
  public String getType() {
    return "ROLE";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.ACCOUNT, ScopeLevel.ORGANIZATION, ScopeLevel.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.empty();
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    return null;
  }
}
