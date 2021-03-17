package io.harness.cdng.k8s;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.delegate.task.k8s.DeleteResourcesType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeleteResourcesWrapper {
  DeleteResourcesType type;
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  DeleteResourcesBaseSpec spec;

  @Builder
  public DeleteResourcesWrapper(DeleteResourcesType type, DeleteResourcesBaseSpec spec) {
    this.type = type;
    this.spec = spec;
  }
}
