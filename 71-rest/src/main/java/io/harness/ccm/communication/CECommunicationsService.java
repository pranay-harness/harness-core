package io.harness.ccm.communication;

import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;

import java.util.List;

public interface CECommunicationsService {
  CECommunications get(String accountId, String email, CommunicationType type);
  List<CECommunications> list(String accountId, String email);
  void update(String accountId, String email, CommunicationType type, boolean enable);
  List<CECommunications> getEnabledEntries(String accountId, CommunicationType type);
}
