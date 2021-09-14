/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.service;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

/**
 * The responsbility of this orchestrator is to decide whether to call the scm api or the jgit
 *  apis
 **/
@OwnedBy(DX)
public interface ScmOrchestratorService extends ScmClient {}
