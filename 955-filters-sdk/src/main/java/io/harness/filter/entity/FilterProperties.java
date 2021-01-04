package io.harness.filter.entity;

import io.harness.filter.FilterType;
import io.harness.filter.dto.FilterPropertiesDTO;
import io.harness.ng.core.common.beans.NGTag;

import java.util.List;
import lombok.Data;

@Data
public abstract class FilterProperties {
  List<NGTag> tags;
  FilterType type;

  public abstract FilterPropertiesDTO writeDTO();
}
