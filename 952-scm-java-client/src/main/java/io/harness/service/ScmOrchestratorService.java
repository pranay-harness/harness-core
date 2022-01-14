package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

/**
 * The responsbility of this orchestrator is to decide whether to call the scm api or the jgit
 *  apis
 **/
@OwnedBy(DX)
public interface ScmOrchestratorService extends ScmClient {}
