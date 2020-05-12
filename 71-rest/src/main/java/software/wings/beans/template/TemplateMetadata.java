package software.wings.beans.template;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY;
import static software.wings.common.TemplateConstants.COPIED_TEMPLATE_METADATA;
import static software.wings.common.TemplateConstants.IMPORTED_TEMPLATE_METADATA;

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
