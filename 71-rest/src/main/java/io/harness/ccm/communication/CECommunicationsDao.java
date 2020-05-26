package io.harness.ccm.communication;

import com.google.inject.Inject;

import io.harness.ccm.communication.entities.CECommunications;
import io.harness.ccm.communication.entities.CECommunications.CECommunicationsKeys;
import io.harness.ccm.communication.entities.CommunicationType;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.FindOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

import java.util.List;

public class CECommunicationsDao {
  @Inject private HPersistence persistence;

  public String save(CECommunications entry) {
    return persistence.save(entry);
  }

  public String save(String accountId, String emailId, CommunicationType type, boolean enable) {
    CECommunications entry =
        CECommunications.builder().accountId(accountId).emailId(emailId).type(type).enabled(enable).build();
    return save(entry);
  }

  public CECommunications get(String accountId, String emailId, CommunicationType type) {
    Query<CECommunications> query = persistence.createQuery(CECommunications.class)
                                        .field(CECommunicationsKeys.accountId)
                                        .equal(accountId)
                                        .field(CECommunicationsKeys.emailId)
                                        .equal(emailId)
                                        .field(CECommunicationsKeys.type)
                                        .equal(type);
    return query.get();
  }

  public List<CECommunications> list(String accountId, String emailId) {
    Query<CECommunications> query = persistence.createQuery(CECommunications.class)
                                        .field(CECommunicationsKeys.accountId)
                                        .equal(accountId)
                                        .field(CECommunicationsKeys.emailId)
                                        .equal(emailId);
    return query.asList(new FindOptions());
  }

  public void update(String accountId, String emailId, CommunicationType type, boolean enable) {
    Query query = persistence.createQuery(CECommunications.class)
                      .field(CECommunicationsKeys.accountId)
                      .equal(accountId)
                      .field(CECommunicationsKeys.emailId)
                      .equal(emailId)
                      .field(CECommunicationsKeys.type)
                      .equal(type);
    UpdateOperations<CECommunications> updateOperations =
        persistence.createUpdateOperations(CECommunications.class).set(CECommunicationsKeys.enabled, enable);
    persistence.update(query, updateOperations);
  }

  public boolean delete(String uuid) {
    return persistence.delete(CECommunications.class, uuid);
  }

  public List<CECommunications> getEnabledEntries(String accountId, CommunicationType type) {
    Query<CECommunications> query = persistence.createQuery(CECommunications.class)
                                        .field(CECommunicationsKeys.accountId)
                                        .equal(accountId)
                                        .field(CECommunicationsKeys.enabled)
                                        .equal(true)
                                        .field(CECommunicationsKeys.type)
                                        .equal(type);
    return query.asList(new FindOptions());
  }
}
