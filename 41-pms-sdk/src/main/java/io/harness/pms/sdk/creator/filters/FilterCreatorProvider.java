package io.harness.pms.sdk.creator.filters;

import java.util.List;

public interface FilterCreatorProvider {
  List<FilterJsonCreator> getFilterJsonCreators();
}
