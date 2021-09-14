/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.cdng.buckets.resources.service;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.delegate.beans.connector.gcpconnector.GcpCredentialType.INHERIT_FROM_DELEGATE;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.IdentifierRef;
import io.harness.cdng.gcp.GcpHelperService;
import io.harness.delegate.beans.connector.gcpconnector.GcpConnectorDTO;
import io.harness.delegate.task.gcp.GcpTaskType;
import io.harness.delegate.task.gcp.request.GcpListBucketsRequest;
import io.harness.delegate.task.gcp.response.GcpBucketDetails;
import io.harness.delegate.task.gcp.response.GcpListBucketsResponse;
import io.harness.ng.core.BaseNGAccess;
import io.harness.security.encryption.EncryptedDataDetail;

import com.google.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@OwnedBy(CDP)
public class GcsResourceServiceImpl implements GcsResourceService {
  @Inject private GcpHelperService gcpHelperService;

  @Override
  public Map<String, String> listBuckets(
      IdentifierRef gcpConnectorRef, String accountId, String orgIdentifier, String projectIdentifier) {
    GcpConnectorDTO gcpConnectorDTO = gcpHelperService.getConnector(gcpConnectorRef);
    BaseNGAccess baseNGAccess = BaseNGAccess.builder()
                                    .accountIdentifier(accountId)
                                    .orgIdentifier(orgIdentifier)
                                    .projectIdentifier(projectIdentifier)
                                    .build();

    List<EncryptedDataDetail> encryptionDetails = gcpHelperService.getEncryptionDetails(gcpConnectorDTO, baseNGAccess);
    GcpListBucketsRequest request =
        GcpListBucketsRequest.builder()
            .gcpManualDetailsDTO(gcpHelperService.getManualDetailsDTO(gcpConnectorDTO))
            .useDelegate(INHERIT_FROM_DELEGATE == gcpConnectorDTO.getCredential().getGcpCredentialType())
            .delegateSelectors(gcpConnectorDTO.getDelegateSelectors())
            .encryptionDetails(encryptionDetails)
            .build();

    GcpListBucketsResponse listBucketsResponse =
        gcpHelperService.executeSyncTask(baseNGAccess, request, GcpTaskType.LIST_BUCKETS, "list GCS buckets");
    return listBucketsResponse.getBuckets().stream().collect(
        Collectors.toMap(GcpBucketDetails::getName, GcpBucketDetails::getId));
  }
}
