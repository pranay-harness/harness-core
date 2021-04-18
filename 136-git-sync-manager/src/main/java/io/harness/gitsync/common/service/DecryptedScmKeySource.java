package io.harness.gitsync.common.service;

import io.harness.beans.IdentifierRef;
import io.harness.delegate.beans.connector.scm.ScmConnector;

public interface DecryptedScmKeySource {
  ScmConnector fetchKey(IdentifierRef scmConnectorRef);

  void removeKey(IdentifierRef scmConnectorRef);
}
