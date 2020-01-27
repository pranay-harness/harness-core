package software.wings.beans.template.command;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static software.wings.common.TemplateConstants.PCF_PLUGIN;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

@JsonTypeName(PCF_PLUGIN)
@Value
@Builder
@JsonInclude(NON_NULL)
public class PcfCommandTemplate implements BaseTemplate {
  private String scriptString;
  @Builder.Default private Integer timeoutIntervalInMinutes = 5;
}
