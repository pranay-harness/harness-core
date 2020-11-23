package io.harness.pms.sample.cv.creator.filters;

import io.harness.pms.sdk.creator.filters.FilterCreatorProvider;
import io.harness.pms.sdk.creator.filters.FilterJsonCreator;
import io.harness.pms.sdk.creator.filters.PipelineFilterJsonCreator;

import java.util.ArrayList;
import java.util.List;

public class CVFilterCreatorProvider implements FilterCreatorProvider {
  @Override
  public List<FilterJsonCreator> getFilterJsonCreators() {
    List<FilterJsonCreator> filterJsonCreators = new ArrayList<>();
    filterJsonCreators.add(new PipelineFilterJsonCreator());
    return filterJsonCreators;
  }
}
