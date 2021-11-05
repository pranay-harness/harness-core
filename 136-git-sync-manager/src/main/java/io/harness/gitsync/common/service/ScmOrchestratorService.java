package io.harness.gitsync.common.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import java.util.function.Function;

@OwnedBy(DX)
public interface ScmOrchestratorService {
  // Caller code eg:
  //    processScmRequest( c->c.listFiles(params);
  <R> R processScmRequest(Function<ScmClientFacilitatorService, R> scmRequest, String projectIdentifier,
      String orgIdentifier, String accountId);

  <R> R processScmRequestUsingConnectorSettings(Function<ScmClientFacilitatorService, R> scmRequest,
      String projectIdentifier, String orgIdentifier, String accountId, String connectorIdentifierRef);

  boolean isExecuteOnDelegate(String projectIdentifier, String orgIdentifier, String accountId);
}
