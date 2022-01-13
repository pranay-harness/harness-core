/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
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
