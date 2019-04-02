package software.wings.graphql.schema.type;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

import java.util.List;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PagedData<T> implements DebugInfo {
  long total;
  int offset;
  int limit;
  List<T> data;
  String debugInfo;
}
