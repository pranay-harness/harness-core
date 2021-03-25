package io.harness.walktree.beans;

import static io.harness.data.structure.HasPredicate.hasNone;

import java.util.ArrayList;
import java.util.List;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class VisitableChildren {
  @Builder.Default List<VisitableChild> visitableChildList = new ArrayList<>();

  public void add(String fieldName, Object value) {
    visitableChildList.add(VisitableChild.builder().fieldName(fieldName).value(value).build());
  }

  public boolean isEmpty() {
    return hasNone(visitableChildList);
  }
}
