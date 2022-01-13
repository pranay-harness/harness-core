/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package io.harness.repositories.gitFileActivity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.gitsync.gitfileactivity.beans.GitFileActivity;

import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.util.List;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;

@OwnedBy(DX)
public interface GitFileActivityRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);

  UpdateResult updateGitFileActivityStatus(GitFileActivity.Status status, String errorMsg, String accountId,
      String commitId, List<String> filePaths, GitFileActivity.Status oldStatus);

  <C> AggregationResults aggregate(Aggregation aggregation, Class<C> castClass);
}
