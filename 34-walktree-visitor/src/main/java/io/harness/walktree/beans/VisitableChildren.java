package io.harness.walktree.beans;

import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
public class VisitableChildren {
  @Builder.Default List<VisitableChild> visitableChildList = new ArrayList<>();

  public void add(String fieldName, Object value) {
    visitableChildList.add(VisitableChild.builder().fieldName(fieldName).value(value).build());
  }

  public boolean isEmpty() {
    return visitableChildList.isEmpty();
  }
}
