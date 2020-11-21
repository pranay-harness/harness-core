package io.harness.ccm.setup;

import static io.harness.persistence.HQuery.excludeValidate;

import io.harness.persistence.HPersistence;

import software.wings.beans.ce.CEMetadataRecord;
import software.wings.beans.ce.CEMetadataRecord.CEMetadataRecordKeys;

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

    if (null != ceMetadataRecord.getGcpConnectorConfigured()) {
      updateOperations.set(CEMetadataRecordKeys.gcpConnectorConfigured, ceMetadataRecord.getGcpConnectorConfigured());
    }

    FindAndModifyOptions findAndModifyOptions = new FindAndModifyOptions().upsert(true).returnNew(true);
    return persistence.upsert(query, updateOperations, findAndModifyOptions);
  }
}
