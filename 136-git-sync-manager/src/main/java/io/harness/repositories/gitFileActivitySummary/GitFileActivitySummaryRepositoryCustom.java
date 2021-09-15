/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package io.harness.repositories.gitFileActivitySummary;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;

import com.mongodb.client.result.DeleteResult;
import java.util.List;

@OwnedBy(DX)
public interface GitFileActivitySummaryRepositoryCustom {
  DeleteResult deleteByIds(List<String> ids);
}
