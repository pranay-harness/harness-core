/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;
import java.util.Set;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedCommand {
  private String commandStoreName;
  private String commandName;
  private String templateId;
  private String name;
  private String appId;
  private String description;
  private String createdAt;
  private String createdBy;
  private String repoUrl;
  private Set<String> tags;
  private List<ImportedCommandVersion> importedCommandVersionList;
  private String highestVersion;
}
