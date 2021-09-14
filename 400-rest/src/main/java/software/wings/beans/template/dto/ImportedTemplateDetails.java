/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package software.wings.beans.template.dto;

import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = HarnessImportedTemplateDetails.class, name = HARNESS_COMMAND_LIBRARY_GALLERY) })
public interface ImportedTemplateDetails {}
