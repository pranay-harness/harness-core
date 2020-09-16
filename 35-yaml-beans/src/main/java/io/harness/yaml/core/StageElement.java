package io.harness.yaml.core;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.harness.beans.ParameterField;
import io.harness.common.SwaggerConstants;
import io.harness.data.validator.EntityIdentifier;
import io.harness.data.validator.EntityName;
import io.harness.visitor.helpers.stage.StageElementHelper;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.auxiliary.intfc.StageElementWrapper;
import io.harness.yaml.core.intfc.StageType;
import io.harness.yaml.core.intfc.WithIdentifier;
import io.swagger.annotations.ApiModelProperty;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonTypeName("stage")
@SimpleVisitorHelper(helperClass = StageElementHelper.class)
public class StageElement implements StageElementWrapper, WithIdentifier, Visitable {
  @EntityIdentifier String identifier;
  @EntityName String name;
  @ApiModelProperty(dataType = SwaggerConstants.STRING_CLASSPATH) ParameterField<String> description;
  String type;
  @JsonProperty("spec")
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  StageType stageType;

  // For Visitor Framework Impl
  String metadata;

  public void setName(String name) {
    this.name = name;
  }

  public void setIdentifier(String identifier) {
    this.identifier = identifier;
  }

  public void setStageType(StageType stageType) {
    this.stageType = stageType;
    if (this.stageType != null) {
      this.stageType.setIdentifier(identifier);
      this.stageType.setName(name);
    }
  }

  @Builder
  public StageElement(
      StageType stageType, String identifier, String name, ParameterField<String> description, String type) {
    this.identifier = identifier;
    this.name = name;
    this.description = description;
    this.type = type;
    this.stageType = stageType;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add("stageType", stageType);
    return children;
  }
}
