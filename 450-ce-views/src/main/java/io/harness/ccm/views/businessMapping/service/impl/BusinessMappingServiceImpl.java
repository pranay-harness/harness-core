/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.views.businessMapping.service.impl;

import io.harness.ccm.views.businessMapping.dao.BusinessMappingDao;
import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.businessMapping.service.intf.BusinessMappingService;
import io.harness.ccm.views.entities.ViewField;
import io.harness.ccm.views.entities.ViewFieldIdentifier;

import com.google.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BusinessMappingServiceImpl implements BusinessMappingService {
  @Inject BusinessMappingDao businessMappingDao;
  @Override
  public boolean save(BusinessMapping businessMapping) {
    return businessMappingDao.save(businessMapping);
  }

  @Override
  public BusinessMapping get(String uuid, String accountId) {
    return businessMappingDao.get(uuid, accountId);
  }

  @Override
  public BusinessMapping get(String uuid) {
    return businessMappingDao.get(uuid);
  }

  @Override
  public BusinessMapping update(BusinessMapping businessMapping) {
    return businessMappingDao.update(businessMapping);
  }

  @Override
  public boolean delete(String uuid, String accountIdentifier) {
    return businessMappingDao.delete(uuid, accountIdentifier);
  }

  @Override
  public List<BusinessMapping> list(String accountId) {
    return businessMappingDao.findByAccountId(accountId);
  }

  @Override
  public List<ViewField> getBusinessMappingViewFields(String accountId) {
    List<BusinessMapping> businessMappingList = businessMappingDao.findByAccountId(accountId);
    List<ViewField> viewFieldList = new ArrayList<>();
    for (BusinessMapping businessMapping : businessMappingList) {
      viewFieldList.add(ViewField.builder()
                            .fieldId(businessMapping.getUuid())
                            .fieldName(businessMapping.getName())
                            .identifier(ViewFieldIdentifier.BUSINESS_MAPPING)
                            .identifierName(ViewFieldIdentifier.BUSINESS_MAPPING.getDisplayName())
                            .build());
    }
    return viewFieldList;
  }
}
