package io.harness.ng.beans;

import static io.harness.NGConstants.PAGE_KEY;
import static io.harness.NGConstants.SIZE_KEY;
import static io.harness.NGConstants.SORT_KEY;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.harness.beans.SortOrder;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

import java.util.List;
import javax.validation.constraints.Max;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
@AllArgsConstructor
public class PageRequest {
  @QueryParam(PAGE_KEY) @DefaultValue("0") int pageIndex;
  @QueryParam(SIZE_KEY) @DefaultValue("50") @Max(100) int pageSize;
  @QueryParam(SORT_KEY) List<SortOrder> sortOrders;
}
