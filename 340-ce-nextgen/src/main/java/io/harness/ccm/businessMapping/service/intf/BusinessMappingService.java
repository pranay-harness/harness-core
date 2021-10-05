package io.harness.ccm.businessMapping.service.intf;

import io.harness.ccm.businessMapping.entities.BusinessMapping;

import java.util.List;

public interface BusinessMappingService {
  boolean save(BusinessMapping businessMapping);
  BusinessMapping get(String uuid, String accountId);
  BusinessMapping update(BusinessMapping businessMapping);
  boolean delete(String uuid, String accountIdentifier);
  List<BusinessMapping> list(String accountId);
}
