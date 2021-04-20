package io.harness.filter.entity;

import static io.harness.annotations.dev.HarnessTeam.DX;

import io.harness.annotations.dev.OwnedBy;
import io.harness.filter.FilterType;
import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Data;

@Data
@OwnedBy(DX)
public abstract class FilterProperties {
  List<NGTag> tags;
  FilterType type;
}
