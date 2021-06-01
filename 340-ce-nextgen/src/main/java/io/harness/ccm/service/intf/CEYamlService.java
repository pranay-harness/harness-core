package io.harness.ccm.service.intf;

import java.io.File;
import java.io.IOException;

public interface CEYamlService {
  File downloadCostOptimisationYaml(String accountId, String connectorIdentifier) throws IOException;
}
