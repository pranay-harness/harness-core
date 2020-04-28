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
