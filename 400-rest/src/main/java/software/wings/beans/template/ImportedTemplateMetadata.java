/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template;

import static software.wings.common.TemplateConstants.IMPORTED_TEMPLATE_METADATA;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;

@JsonTypeName(IMPORTED_TEMPLATE_METADATA)
@Value
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ImportedTemplateMetadata implements TemplateMetadata {
  private Long defaultVersion;
}
