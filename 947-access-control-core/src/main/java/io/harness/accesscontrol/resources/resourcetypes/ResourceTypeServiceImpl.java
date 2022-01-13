/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

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
