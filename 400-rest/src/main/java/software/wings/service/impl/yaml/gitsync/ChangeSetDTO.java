/*
 * Copyright 2021 Harness Inc. All rights reserved.
 * Use of this source code is governed by the Polyform Free Trial v1.0 license
 * that can be found in the LICENSE file for this repository.
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
