/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.communication;

import static io.harness.annotations.dev.HarnessTeam.CE;

import io.harness.annotations.dev.HarnessModule;
import io.harness.annotations.dev.OwnedBy;
import io.harness.annotations.dev.TargetModule;
import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CommunicationType;

import java.util.List;
import java.util.Map;

@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
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
