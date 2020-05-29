package io.harness.beans.steps.stepinfo;

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;
import io.harness.beans.steps.AbstractStepWithMetaInfo;
import io.harness.beans.steps.StepInfoType;
import io.harness.beans.steps.TypeInfo;
import io.harness.state.StepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import software.wings.jersey.JsonViews;

import java.util.List;
import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@JsonTypeName("upload")
public class UploadStepInfo extends AbstractStepWithMetaInfo {
  @JsonView(JsonViews.Internal.class)
  @NotNull
  public static final TypeInfo typeInfo = TypeInfo.builder()
                                              .stepInfoType(StepInfoType.UPLOAD)
                                              .stepType(StepType.builder().type(StepInfoType.UPLOAD.name()).build())
                                              .build();
  @NotNull List<Item> items;

  @Builder
  public UploadStepInfo(String type, String identifier, String name, List<String> dependencies, Integer retry,
      Integer timeout, List<Item> items) {
    super(type, identifier, name, dependencies, retry, timeout);
    this.items = items;
  }

  @Override
  public TypeInfo getNonYamlInfo() {
    return typeInfo;
  }

  @Data
  @Builder
  @NoArgsConstructor
  @AllArgsConstructor
  private static class Item {
    private String filePattern;
    private String destination;
    private String connector;
  }
}
