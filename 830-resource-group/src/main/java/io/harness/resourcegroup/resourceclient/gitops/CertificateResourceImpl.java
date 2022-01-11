package io.harness.resourcegroup.resourceclient.gitops;

import io.harness.beans.Scope;
import io.harness.beans.ScopeLevel;
import io.harness.eventsframework.consumer.Message;
import io.harness.resourcegroup.beans.ValidatorType;
import io.harness.resourcegroup.framework.service.Resource;
import io.harness.resourcegroup.framework.service.ResourceInfo;

import com.google.inject.Singleton;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Singleton
public class CertificateResourceImpl implements Resource {
  @Override
  public String getType() {
    return "GITOPS_CERT";
  }

  @Override
  public Set<ScopeLevel> getValidScopeLevels() {
    return EnumSet.of(ScopeLevel.PROJECT);
  }

  @Override
  public Optional<String> getEventFrameworkEntityType() {
    return Optional.empty();
  }

  @Override
  public ResourceInfo getResourceInfoFromEvent(Message message) {
    return null;
  }

  @Override
  public List<Boolean> validate(List<String> resourceIds, Scope scope) {
    return null;
  }

  @Override
  public EnumSet<ValidatorType> getSelectorKind() {
    return EnumSet.of(ValidatorType.DYNAMIC);
  }
}
