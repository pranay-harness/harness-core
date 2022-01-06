/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Shield 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/06/PolyForm-Shield-1.0.0.txt.
 */

package io.harness.provision;

import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@Builder
@EqualsAndHashCode
public class TfVarScriptRepositorySource implements TfVarSource {
  private List<String> tfVarFilePaths;

  @Override
  public TfVarSourceType getTfVarSourceType() {
    return TfVarSourceType.SCRIPT_REPOSITORY;
  }
}
