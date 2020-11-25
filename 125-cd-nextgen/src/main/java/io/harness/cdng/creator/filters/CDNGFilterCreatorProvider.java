package io.harness.cdng.creator.filters;

import io.harness.pms.sdk.creator.filters.FilterCreatorProvider;
import io.harness.pms.sdk.creator.filters.FilterJsonCreator;

import java.util.ArrayList;
import java.util.List;

public class CDNGFilterCreatorProvider implements FilterCreatorProvider {
  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    return new ArrayList<>();
  }
}
