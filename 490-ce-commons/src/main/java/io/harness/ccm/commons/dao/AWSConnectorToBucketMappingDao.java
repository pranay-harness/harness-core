/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.ccm.commons.dao;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.persistence.HPersistence.upsertReturnNewOptions;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.commons.entities.AWSConnectorToBucketMapping;
import io.harness.ccm.commons.entities.AWSConnectorToBucketMapping.AWSConnectorToBucketMappingKeys;
import io.harness.persistence.HPersistence;

import com.google.inject.Inject;
import org.mongodb.morphia.query.Query;
import org.mongodb.morphia.query.UpdateOperations;

@OwnedBy(CE)
public class AWSConnectorToBucketMappingDao {
  private final HPersistence persistence;
  @Inject
  public AWSConnectorToBucketMappingDao(HPersistence persistence) {
    this.persistence = persistence;
  }

  public AWSConnectorToBucketMapping upsert(AWSConnectorToBucketMapping awsConnectorToBucketMapping) {
    Query<AWSConnectorToBucketMapping> query =
        persistence.createQuery(AWSConnectorToBucketMapping.class)
            .filter(AWSConnectorToBucketMappingKeys.accountId, awsConnectorToBucketMapping.getAccountId())
            .filter(AWSConnectorToBucketMappingKeys.awsConnectorIdentifier,
                awsConnectorToBucketMapping.getAwsConnectorIdentifier());

    UpdateOperations<AWSConnectorToBucketMapping> updateOperations =
        persistence.createUpdateOperations(AWSConnectorToBucketMapping.class)
            .set(AWSConnectorToBucketMappingKeys.accountId, awsConnectorToBucketMapping.getAccountId())
            .set(AWSConnectorToBucketMappingKeys.awsConnectorIdentifier,
                awsConnectorToBucketMapping.getAwsConnectorIdentifier())
            .set(AWSConnectorToBucketMappingKeys.destinationBucket, awsConnectorToBucketMapping.getDestinationBucket());

    return persistence.upsert(query, updateOperations, upsertReturnNewOptions);
  }

  public AWSConnectorToBucketMapping getByAwsConnectorId(String accountId, String awsConnectorIdentifier) {
    Query<AWSConnectorToBucketMapping> query =
        persistence.createQuery(AWSConnectorToBucketMapping.class)
            .filter(AWSConnectorToBucketMappingKeys.accountId, accountId)
            .filter(AWSConnectorToBucketMappingKeys.awsConnectorIdentifier, awsConnectorIdentifier);
    return query.get();
  }
}
