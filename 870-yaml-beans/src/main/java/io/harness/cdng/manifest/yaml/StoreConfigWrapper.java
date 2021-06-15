package io.harness.cdng.manifest.yaml;

import static com.fasterxml.jackson.annotation.JsonTypeInfo.As.EXTERNAL_PROPERTY;
import static com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME;
import static java.lang.String.format;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.cdng.visitor.helpers.manifest.StoreConfigWrapperVisitorHelper;
import io.harness.exception.UnexpectedTypeException;
import io.harness.pms.yaml.YAMLFieldNameConstants;
import io.harness.walktree.beans.VisitableChildren;
import io.harness.walktree.visitor.SimpleVisitorHelper;
import io.harness.walktree.visitor.Visitable;
import io.harness.yaml.core.intfc.OverridesApplier;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import javax.validation.constraints.NotNull;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.Wither;
import org.springframework.data.annotation.TypeAlias;

@OwnedBy(HarnessTeam.CDP)
@Data
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
@SimpleVisitorHelper(helperClass = StoreConfigWrapperVisitorHelper.class)
@TypeAlias("storeConfigWrapper")
public class StoreConfigWrapper implements OverridesApplier<StoreConfigWrapper>, Visitable {
  @NotNull String type;
  @NotNull
  @JsonTypeInfo(use = NAME, property = "type", include = EXTERNAL_PROPERTY, visible = true)
  @Wither
  StoreConfig spec;

  @Builder
  public StoreConfigWrapper(String type, StoreConfig spec) {
    this.type = type;
    this.spec = spec;
  }

  @Override
  public StoreConfigWrapper applyOverrides(StoreConfigWrapper overrideConfig) {
    StoreConfigWrapper resultantConfig = this;
    if (overrideConfig != null) {
      if (!overrideConfig.getType().equals(resultantConfig.getType())) {
        throw new UnexpectedTypeException(format("Unable to apply store override of type '%s' to store of type '%s'",
            overrideConfig.getType(), resultantConfig.getType()));
      }

      resultantConfig = resultantConfig.withSpec(spec.applyOverrides(overrideConfig.getSpec()));
    }
    return resultantConfig;
  }

  @Override
  public VisitableChildren getChildrenToWalk() {
    VisitableChildren children = VisitableChildren.builder().build();
    children.add(YAMLFieldNameConstants.SPEC, spec);
    return children;
  }
}
