package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.script.ScriptInfo;
import io.harness.beans.steps.CIStepInfo;
import io.harness.beans.steps.CIStepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.state.StepType;
import lombok.Builder;
import lombok.Value;
import org.hibernate.validator.constraints.NotEmpty;
import software.wings.jersey.JsonViews;

import java.beans.ConstructorProperties;
import java.util.List;
import java.util.Optional;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

@Value
@JsonTypeName("test")
public class TestStepInfo implements CIStepInfo {
  public static final int DEFAULT_RETRY = 0;
  public static final int DEFAULT_TIMEOUT = 1200;

  @JsonView(JsonViews.Internal.class)
  @NotNull
  private static final TypeInfo typeInfo = TypeInfo.builder()
                                               .stepInfoType(CIStepInfoType.TEST)
                                               .stepType(StepType.builder().type(CIStepInfoType.TEST.name()).build())
                                               .build();

  @NotNull String identifier;
  String name;
  @Min(MIN_RETRY) @Max(MAX_RETRY) int retry;
  @Min(MIN_TIMEOUT) @Max(MAX_TIMEOUT) int timeout;

  Test test;

  @Builder
  @ConstructorProperties({"identifier", "name", "retry", "timeout", "test"})
  public TestStepInfo(String identifier, String name, Integer retry, Integer timeout, Test test) {
    this.identifier = identifier;
    this.name = name;
    this.retry = Optional.ofNullable(retry).orElse(DEFAULT_RETRY);
    this.timeout = Optional.ofNullable(timeout).orElse(DEFAULT_TIMEOUT);
    this.test = test;
  }

  @Value
  @Builder
  public static class Test {
    @NotEmpty String numParallel;
    List<ScriptInfo> scriptInfos;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }
}