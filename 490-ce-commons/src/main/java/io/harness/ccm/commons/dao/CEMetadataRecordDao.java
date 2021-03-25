package io.harness.ccm.commons.dao;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.ccm.commons.entities.CEMetadataRecord;
import io.harness.ccm.commons.entities.CEMetadataRecord.CEMetadataRecordKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.FindAndModifyOptions;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

public class CEMetadataRecordDao {
  private final HPersistence persistence;

  @Inject
  public CEMetadataRecordDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public CEMetadataRecord upsert(CEMetadataRecord ceMetadataRecord) {
    Query<CEMetadataRecord> query = persistence.createQuery(CEMetadataRecord.class, excludeValidate)
                                        .filter(CEMetadataRecordKeys.accountId, ceMetadataRecord.getAccountId());

    UpdateOperations<CEMetadataRecord> updateOperations = persistence.createUpdateOperations(CEMetadataRecord.class);

    if (null != ceMetadataRecord.getClusterDataConfigured()) {
      updateOperations.set(CEMetadataRecordKeys.clusterDataConfigured, ceMetadataRecord.getClusterDataConfigured());
    }

    if (null != ceMetadataRecord.getAwsConnectorConfigured()) {
      updateOperations.set(CEMetadataRecordKeys.awsConnectorConfigured, ceMetadataRecord.getAwsConnectorConfigured());
    }

    if (null != ceMetadataRecord.getAwsDataPresent()) {
      updateOperations.set(CEMetadataRecordKeys.awsDataPresent, ceMetadataRecord.getAwsDataPresent());
    }

    if (null != ceMetadataRecord.getGcpConnectorConfigured()) {
      updateOperations.set(CEMetadataRecordKeys.gcpConnectorConfigured, ceMetadataRecord.getGcpConnectorConfigured());
    }

    if (null != ceMetadataRecord.getGcpDataPresent()) {
      updateOperations.set(CEMetadataRecordKeys.gcpDataPresent, ceMetadataRecord.getGcpDataPresent());
    }

    if (null != ceMetadataRecord.getAzureConnectorConfigured()) {
      updateOperations.set(
          CEMetadataRecordKeys.azureConnectorConfigured, ceMetadataRecord.getAzureConnectorConfigured());
    }

    if (null != ceMetadataRecord.getAzureDataPresent()) {
      updateOperations.set(CEMetadataRecordKeys.azureDataPresent, ceMetadataRecord.getAzureDataPresent());
    }

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }

  public CEMetadataRecord getByAccountId(String accountId) {
    return persistence.createQuery(CEMetadataRecord.class).field(CEMetadataRecordKeys.accountId).equal(accountId).get();
  }
}
