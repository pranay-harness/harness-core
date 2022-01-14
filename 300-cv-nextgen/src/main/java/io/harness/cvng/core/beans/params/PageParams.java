package io.harness.cvng.core.beans.params;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Value;
import lombok.experimental.FieldDefaults;
import lombok.experimental.SuperBuilder;

@FieldDefaults(level = AccessLevel.PRIVATE)
@Value
@SuperBuilder
@NoArgsConstructor
public class PageParams {
  @QueryParam("pageNumber") @DefaultValue("0") @NonNull int page;
  @QueryParam("pageSize") @DefaultValue("10") @NonNull int size;
}
