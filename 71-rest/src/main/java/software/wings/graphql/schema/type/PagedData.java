package software.wings.graphql.schema.type;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class PagedData<T> implements DebugInfo {
  long total;
  int offset;
  int limit;
  List<T> data;
  String debugInfo;
}
