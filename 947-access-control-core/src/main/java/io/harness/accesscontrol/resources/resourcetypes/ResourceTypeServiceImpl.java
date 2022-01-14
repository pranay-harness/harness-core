package io.harness.accesscontrol.resources.resourcetypes;

import io.harness.accesscontrol.resources.resourcetypes.persistence.ResourceTypeDao;
import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.util.List;
import java.util.Optional;
import javax.validation.executable.ValidateOnExecution;

@OwnedBy(HarnessTeam.PL)
@Singleton
@ValidateOnExecution
public class ResourceTypeServiceImpl implements ResourceTypeService {
  private final ResourceTypeDao resourceTypeDao;

  @Inject
  public ResourceTypeServiceImpl(ResourceTypeDao resourceTypeDao) {
    this.resourceTypeDao = resourceTypeDao;
  }

  @Override
  public ResourceType save(ResourceType resourceType) {
    return resourceTypeDao.save(resourceType);
  }

  @Override
  public Optional<ResourceType> get(String identifier) {
    return resourceTypeDao.get(identifier);
  }

  @Override
  public Optional<ResourceType> getByPermissionKey(String permissionKey) {
    return resourceTypeDao.getByPermissionKey(permissionKey);
  }

  @Override
  public List<ResourceType> list() {
    return resourceTypeDao.list();
  }
}
