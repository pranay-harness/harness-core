package software.wings.service.impl.aws.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AwsCFTemplateParamsData {
  private String paramKey;
  private String paramType;
  private String defaultValue;
}