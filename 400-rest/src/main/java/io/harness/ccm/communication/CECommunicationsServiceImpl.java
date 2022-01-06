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

import software.wings.service.intfc.ce.CeAccountExpirationChecker;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CE)
@TargetModule(HarnessModule._490_CE_COMMONS)
public class CECommunicationsServiceImpl implements CECommunicationsService {
  @Inject CECommunicationsDao ceCommunicationsDao;
  @Inject CeAccountExpirationChecker accountChecker;

  @Override
  public CECommunications get(String accountId, String email, CommunicationType type) {
    return ceCommunicationsDao.get(accountId, email, type);
  }

  @Override
  public List<CECommunications> list(String accountId, String email) {
    return ceCommunicationsDao.list(accountId, email);
  }

  @Override
  public void update(String accountId, String email, CommunicationType type, boolean enable, boolean selfEnabled) {
    accountChecker.checkIsCeEnabled(accountId);
    CECommunications entry = get(accountId, email, type);
    if (entry == null) {
      entry = CECommunications.builder()
                  .accountId(accountId)
                  .emailId(email)
                  .type(type)
                  .enabled(enable)
                  .selfEnabled(selfEnabled)
                  .build();
      ceCommunicationsDao.save(entry);
    } else {
      ceCommunicationsDao.update(accountId, email, type, enable);
    }
  }

  @Override
  public void delete(String accountId, String email, CommunicationType type) {
    accountChecker.checkIsCeEnabled(accountId);
    CECommunications entry = get(accountId, email, type);
    if (entry != null) {
      ceCommunicationsDao.delete(entry.getUuid());
    }
  }

  @Override
  public List<CECommunications> getEntriesEnabledViaEmail(String accountId) {
    return ceCommunicationsDao.getEntriesEnabledViaEmail(accountId);
  }

  public List<CECommunications> getEnabledEntries(String accountId, CommunicationType type) {
    return ceCommunicationsDao.getEnabledEntries(accountId, type);
  }

  @Override
  public void unsubscribe(String id) {
    CECommunications entry = ceCommunicationsDao.get(id);
    if (entry != null) {
      ceCommunicationsDao.update(entry.getAccountId(), entry.getEmailId(), entry.getType(), false);
    }
  }

  @Override
  public Map<String, String> getUniqueIdPerUser(String accountId, CommunicationType type) {
    List<CECommunications> enabledEntries = ceCommunicationsDao.getEnabledEntries(accountId, type);
    return enabledEntries.stream().collect(Collectors.toMap(CECommunications::getEmailId, CECommunications::getUuid));
  }
}
