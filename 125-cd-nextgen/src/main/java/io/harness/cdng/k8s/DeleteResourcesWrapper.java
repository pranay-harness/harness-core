package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotation.RecasterAlias;
import io.harness.annotations.dev.OwnedBy;
import io.harness.delegate.task.k8s.DeleteResourcesType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@OwnedBy(CDP)
@Data
@NoArgsConstructor
@RecasterAlias("io.harness.cdng.k8s.DeleteResourcesWrapper")
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
