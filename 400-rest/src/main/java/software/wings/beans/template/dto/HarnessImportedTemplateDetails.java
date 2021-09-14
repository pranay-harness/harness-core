/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template.dto;

import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Set;
import lombok.Builder;
import lombok.Data;

@JsonTypeName(HARNESS_COMMAND_LIBRARY_GALLERY)
@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(NON_NULL)
public class HarnessImportedTemplateDetails implements ImportedTemplateDetails {
  private String commandVersion;
  private String commandName;
  private String commandStoreName;
  private transient String repoUrl;
  private transient Set<String> tags;
}
