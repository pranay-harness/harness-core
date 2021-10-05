package io.harness.ccm.businessMapping.service.impl;

import io.harness.ccm.businessMapping.dao.BusinessMappingDao;
import io.harness.ccm.businessMapping.entities.BusinessMapping;
import io.harness.ccm.businessMapping.service.intf.BusinessMappingService;

import com.google.inject.Inject;
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
}
