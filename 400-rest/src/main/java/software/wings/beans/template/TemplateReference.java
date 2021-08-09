package software.wings.beans.template;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class TemplateReference {
  private String templateUuid;
  private Long templateVersion;
}
