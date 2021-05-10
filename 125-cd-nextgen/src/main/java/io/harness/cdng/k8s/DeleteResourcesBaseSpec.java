package io.harness.cdng.k8s;

import static io.harness.annotations.dev.HarnessTeam.CDP;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import io.harness.annotations.dev.OwnedBy;
import io.harness.beans.common.SwaggerConstants;
import io.harness.delegate.task.k8s.DeleteResourcesType;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.annotations.ApiModelProperty;

@OwnedBy(CDP)
@JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
public interface DeleteResourcesBaseSpec {
  DeleteResourcesType getType();
  @ApiModelProperty(dataType = SwaggerConstants.STRING_LIST_CLASSPATH)
  @JsonInclude(JsonInclude.Include.NON_EMPTY)
  String getResourceNames();
  @JsonInclude(JsonInclude.Include.NON_EMPTY) String getManifestPaths();
  @JsonInclude(JsonInclude.Include.NON_NULL) Boolean getDeleteNamespace();
  @JsonInclude(JsonInclude.Include.NON_NULL) Boolean getAllManifestPaths();
}
