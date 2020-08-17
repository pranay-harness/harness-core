package io.harness.gitsync.common.service;

import io.harness.gitsync.common.dtos.GitSyncEntityListDTO;

import java.util.List;

public interface GitEntityService { List<GitSyncEntityListDTO> list(String projectId, String orgId, String accountId); }
