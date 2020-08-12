package io.harness.ccm.communication;

import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;

import java.util.List;
import java.util.Map;

public interface CECommunicationsService {
  CECommunications get(String accountId, String email, CommunicationType type);
  List<CECommunications> list(String accountId, String email);
  void update(String accountId, String email, CommunicationType type, boolean enable, boolean selfEnabled);
  List<CECommunications> getEnabledEntries(String accountId, CommunicationType type);
  void delete(String accountId, String email, CommunicationType type);
  List<CECommunications> getEntriesEnabledViaEmail(String accountId);
  void unsubscribe(String id);
  Map<String, String> getUniqueIdPerUser(String accountId, CommunicationType type);
}
