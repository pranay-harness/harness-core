/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
 */

package software.wings.service.impl.yaml.gitdiff;

import software.wings.beans.yaml.GitFileChange;

import java.util.List;
import java.util.Map;
import lombok.Builder;
import lombok.Singular;
import lombok.Value;

@Value
@Builder
public class YamlFilterResult {
  @Singular List<GitFileChange> filteredFiles;
  @Singular("excludedFilePathWithReason") Map<String, String> excludedFilePathWithReasonMap;
}
