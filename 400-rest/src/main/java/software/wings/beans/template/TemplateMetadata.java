/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

package software.wings.beans.template;

import static software.wings.common.TemplateConstants.COPIED_TEMPLATE_METADATA;
import static software.wings.common.TemplateConstants.IMPORTED_TEMPLATE_METADATA;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = PROPERTY)
@JsonSubTypes({
  @JsonSubTypes.Type(value = CopiedTemplateMetadata.class, name = COPIED_TEMPLATE_METADATA)
  , @JsonSubTypes.Type(value = ImportedTemplateMetadata.class, name = IMPORTED_TEMPLATE_METADATA)
})
public interface TemplateMetadata {}
