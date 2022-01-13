/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.service.impl.yaml.gitsync;

import software.wings.beans.GitDetail;
import software.wings.yaml.gitSync.YamlChangeSet;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.FieldNameConstants;

@Value
@Builder
@FieldNameConstants(innerTypeName = "OngoingCommitsDTOKeys")
@JsonIgnoreProperties(ignoreUnknown = true)
public class ChangeSetDTO {
  GitDetail gitDetail;
  YamlChangeSet.Status status;
  String changeSetId;
  boolean gitToHarness;
  @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "status", include = JsonTypeInfo.As.EXTERNAL_PROPERTY)
  ChangesetInformation changesetInformation;
}
