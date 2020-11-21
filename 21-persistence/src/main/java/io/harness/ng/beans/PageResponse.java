package io.harness.ng.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonIgnoreProperties(ignoreUnknown = true)
@ApiModel(value = "Page")
public class PageResponse<T> {
  long totalPages;
  long totalItems;
  long pageItemCount;
  long pageSize;
  List<T> content;
  long pageIndex;
  boolean empty;
}
