package io.harness.ccm.views.businessMapping.service.intf;

import io.harness.ccm.views.businessMapping.entities.BusinessMapping;
import io.harness.ccm.views.entities.ViewField;

import java.util.List;

public interface BusinessMappingService {
  boolean save(io.harness.ccm.views.businessMapping.entities.BusinessMapping businessMapping);
  io.harness.ccm.views.businessMapping.entities.BusinessMapping get(String uuid, String accountId);
  io.harness.ccm.views.businessMapping.entities.BusinessMapping update(
      io.harness.ccm.views.businessMapping.entities.BusinessMapping businessMapping);
  boolean delete(String uuid, String accountIdentifier);
  List<BusinessMapping> list(String accountId);
  List<ViewField> getBusinessMappingViewFields(String accountId);
}
