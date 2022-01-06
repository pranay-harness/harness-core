/*
 * Copyright 2022 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.ccm.billing.bigquery;

import static io.harness.annotations.dev.HarnessTeam.CE;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.CE_GCP_CREDENTIALS_PATH;
import static io.harness.ccm.billing.GcpServiceAccountServiceImpl.getImpersonatedCredentials;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ccm.billing.GcpServiceAccountServiceImpl;

import software.wings.beans.ValidationResult;

import com.google.auth.Credentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.BigQueryException;
import com.google.cloud.bigquery.BigQueryOptions;
import com.google.cloud.bigquery.Dataset;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
@OwnedBy(CE)
public class BigQueryServiceImpl implements BigQueryService, io.harness.ccm.bigQuery.BigQueryService {
  @Override
  public BigQuery get() {
    return get(null, null);
  }

  @Override
  public BigQuery get(String projectId, String impersonatedServiceAccount) {
    ServiceAccountCredentials sourceCredentials = getCredentials(CE_GCP_CREDENTIALS_PATH);
    Credentials credentials = getImpersonatedCredentials(sourceCredentials, impersonatedServiceAccount);

    BigQueryOptions.Builder bigQueryOptionsBuilder = BigQueryOptions.newBuilder().setCredentials(credentials);

    if (projectId != null) {
      bigQueryOptionsBuilder.setProjectId(projectId);
    }
    return bigQueryOptionsBuilder.build().getService();
  }

  @Override
  public ValidationResult canAccessDataset(BigQuery bigQuery, String projectId, String datasetId) {
    try {
      Dataset dataset = bigQuery.getDataset(datasetId);
      if (dataset == null) {
        return ValidationResult.builder()
            .valid(false)
            .errorMessage(format("Unable to find the dataset \"%s\".", datasetId))
            .build();
      } else {
        return ValidationResult.builder().valid(true).build();
      }
    } catch (BigQueryException be) {
      log.error("Unable to access BigQuery Dataset", be);
      return ValidationResult.builder().valid(false).errorMessage(be.getMessage()).build();
    }
  }

  @Override
  public ServiceAccountCredentials getCredentials(String googleCredentialPathSystemEnv) {
    return GcpServiceAccountServiceImpl.getCredentials(googleCredentialPathSystemEnv);
  }
}
