/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template;

import static software.wings.common.TemplateConstants.COPIED_TEMPLATE_METADATA;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@JsonTypeName(COPIED_TEMPLATE_METADATA)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopiedTemplateMetadata implements TemplateMetadata {
  private String parentTemplateId;
  private Long parentTemplateVersion;
  private String parentCommandVersion;
  private String parentCommandName;
  private String parentCommandStoreName;
}
