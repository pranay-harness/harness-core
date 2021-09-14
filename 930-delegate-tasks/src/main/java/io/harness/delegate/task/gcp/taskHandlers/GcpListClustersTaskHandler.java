/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.delegate.task.gcp.taskHandlers;

import static io.harness.annotations.dev.HarnessTeam.CDP;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.lang.String.format;

import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.beans.connector.gcpconnector.GcpManualDetailsDTO;
import io.harness.delegate.task.gcp.helpers.GkeClusterHelper;
import io.harness.delegate.task.gcp.request.GcpListClustersRequest;
import io.harness.delegate.task.gcp.request.GcpRequest;
import io.harness.delegate.task.gcp.response.GcpClusterListTaskResponse;
import io.harness.delegate.task.gcp.response.GcpResponse;
import io.harness.errorhandling.NGErrorHelper;
import io.harness.exception.InvalidRequestException;
import io.harness.logging.CommandExecutionStatus;
import io.harness.security.encryption.SecretDecryptionService;

import com.google.inject.Inject;
import java.util.List;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@NoArgsConstructor
@Slf4j
@OwnedBy(CDP)
public class GcpListClustersTaskHandler implements TaskHandler {
  @Inject private SecretDecryptionService secretDecryptionService;
  @Inject private NGErrorHelper ngErrorHelper;
  @Inject private GkeClusterHelper gkeClusterHelper;

  @Override
  public GcpResponse executeRequest(GcpRequest gcpRequest) {
    try {
      return getClusterNames(gcpRequest);
    } catch (Exception exception) {
      log.error("Failed retrieving GCP cluster list.", exception);
      return failureResponse(exception);
    }
  }

  private GcpClusterListTaskResponse getClusterNames(GcpRequest gcpRequest) {
    if (!(gcpRequest instanceof GcpListClustersRequest)) {
      throw new InvalidRequestException(
          format("Invalid GCP request type, expecting: %s", GcpListClustersRequest.class));
    }

    GcpListClustersRequest request = (GcpListClustersRequest) gcpRequest;
    boolean useDelegate = request.getGcpManualDetailsDTO() == null && isNotEmpty(request.getDelegateSelectors());
    List<String> clusterNames = gkeClusterHelper.listClusters(getGcpServiceAccountKeyFileContent(request), useDelegate);

    return GcpClusterListTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.SUCCESS)
        .clusterNames(clusterNames)
        .build();
  }

  private GcpClusterListTaskResponse failureResponse(Exception ex) {
    return GcpClusterListTaskResponse.builder()
        .commandExecutionStatus(CommandExecutionStatus.FAILURE)
        .errorMessage(ngErrorHelper.getErrorSummary(ex.getMessage()))
        .errorDetail(ngErrorHelper.createErrorDetail(ex.getMessage()))
        .build();
  }

  private char[] getGcpServiceAccountKeyFileContent(GcpListClustersRequest request) {
    GcpManualDetailsDTO gcpManualDetailsDTO = request.getGcpManualDetailsDTO();
    if (gcpManualDetailsDTO != null) {
      secretDecryptionService.decrypt(gcpManualDetailsDTO, request.getEncryptionDetails());
      return gcpManualDetailsDTO.getSecretKeyRef().getDecryptedValue();
    }

    return null;
  }
}
