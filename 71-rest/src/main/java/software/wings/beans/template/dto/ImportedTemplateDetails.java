package software.wings.beans.template.dto;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static software.wings.common.TemplateConstants.HARNESS_COMMAND_LIBRARY_GALLERY;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes(
    { @JsonSubTypes.Type(value = HarnessImportedTemplateDetails.class, name = HARNESS_COMMAND_LIBRARY_GALLERY) })
public interface ImportedTemplateDetails {}
