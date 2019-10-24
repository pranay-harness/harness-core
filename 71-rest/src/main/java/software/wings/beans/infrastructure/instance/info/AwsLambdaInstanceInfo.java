package software.wings.beans.infrastructure.instance.info;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import software.wings.beans.Tag;
import software.wings.beans.infrastructure.instance.InvocationCount;

import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
@EqualsAndHashCode(callSuper = true)
public class AwsLambdaInstanceInfo extends ServerlessInstanceInfo {
  private String functionName;
  private String version;
  private Set<String> aliases;
  private Set<Tag> tags;
  private String functionArn;
  private String description;
  private String runtime;
  private String handler;

  @Builder
  public AwsLambdaInstanceInfo(List<InvocationCount> invocationCountList, String functionName, String version,
      Set<String> aliases, Set<Tag> tags, String functionArn, String description, String runtime, String handler) {
    super(emptyIfNull(invocationCountList)
              .stream()
              .collect(Collectors.toMap(InvocationCount::getKey, Function.identity())));
    this.functionName = functionName;
    this.version = version;
    this.aliases = aliases;
    this.tags = tags;
    this.functionArn = functionArn;
    this.description = description;
    this.runtime = runtime;
    this.handler = handler;
  }
}
