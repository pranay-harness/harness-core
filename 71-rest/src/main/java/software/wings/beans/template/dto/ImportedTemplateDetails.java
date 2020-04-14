package software.wings.beans.template.dto;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonIgnoreProperties(ignoreUnknown = true)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = EXTERNAL_PROPERTY)
@JsonSubTypes(value =
    { @JsonSubTypes.Type(value = HarnessImportedTemplateDetails.class, name = "HARNESS_COMMAND_LIBRARY_GALLERY") })
public interface ImportedTemplateDetails {}
