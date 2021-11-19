package io.harness.ccm.service.impl;

import io.harness.ccm.views.dao.CEViewDao;
import io.harness.ccm.views.entities.ViewType;
import io.harness.enforcement.beans.metadata.RestrictionMetadataDTO;
import io.harness.enforcement.client.usage.RestrictionUsageInterface;

import com.google.inject.Inject;

public class PerspectivesRestrictionUsageImpl implements RestrictionUsageInterface {
  @Inject CEViewDao viewDao;
  @Override
  public long getCurrentValue(String accountIdentifier, RestrictionMetadataDTO restrictionMetadataDTO) {
    return viewDao.findCountByAccountIdAndType(accountIdentifier, ViewType.CUSTOMER);
  }
}
