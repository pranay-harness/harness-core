package software.wings.beans.template.command;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import software.wings.beans.template.BaseTemplate;

@JsonTypeName("HTTP")
@Value
@Builder
@JsonInclude(NON_NULL)
public class HttpTemplate implements BaseTemplate {
  private String url;
  private String method;
  private String header;
  private String body;
  private String assertion;
  private transient boolean executeWithPreviousSteps;
  private int socketTimeoutMillis = 10000;
}
