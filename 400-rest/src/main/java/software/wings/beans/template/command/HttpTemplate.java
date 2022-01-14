package software.wings.beans.template.command;

import static io.harness.annotations.dev.HarnessTeam.CDC;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.KeyValuePair;

import software.wings.beans.template.BaseTemplate;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@OwnedBy(CDC)
@JsonTypeName("HTTP")
@Data
@Builder
@JsonInclude(NON_NULL)
public class HttpTemplate implements BaseTemplate {
  private String url;
  private String method;
  private String header;
  private List<KeyValuePair> headers;
  private String body;
  private String assertion;
  private transient boolean executeWithPreviousSteps;
  @Builder.Default private int timeoutMillis = 10000;
}
