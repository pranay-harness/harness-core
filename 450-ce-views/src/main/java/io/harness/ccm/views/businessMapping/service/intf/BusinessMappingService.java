package io.harness.ccm.views.businessMapping.service.intf;

import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.entities.ViewField;

import java.util.List;

public interface BusinessMappingService {
  boolean save(BusinessMapping businessMapping);
  BusinessMapping get(String uuid, String accountId);
  BusinessMapping get(String uuid);
  BusinessMapping update(BusinessMapping businessMapping);
  boolean delete(String uuid, String accountIdentifier);
  List<BusinessMapping> list(String accountId);
  List<ViewField> getBusinessMappingViewFields(String accountId);
}
