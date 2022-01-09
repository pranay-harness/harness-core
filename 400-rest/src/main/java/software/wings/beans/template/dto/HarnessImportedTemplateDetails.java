/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
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
