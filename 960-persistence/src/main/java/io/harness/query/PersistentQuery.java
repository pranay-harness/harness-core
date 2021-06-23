package io.harness.query;

import com.google.common.collect.ImmutableList;
import java.util.List;

public interface PersistentQuery {
  default List<String> queryCanonicalForms() {
    return ImmutableList.<String>builder().build();
  }
}
