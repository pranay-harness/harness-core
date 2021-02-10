package io.harness.ng.beans;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.annotations.ApiModel;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;
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

  public <U> PageResponse<U> map(Function<? super T, ? extends U> converter) {
    List<U> convertedContent = this.content.stream().map(converter).collect(Collectors.toList());
    return new PageResponseBuilder<U>()
        .totalPages(this.totalPages)
        .totalItems(this.totalItems)
        .pageItemCount(this.pageItemCount)
        .pageSize(this.pageSize)
        .content(convertedContent)
        .pageIndex(this.pageIndex)
        .empty(this.empty)
        .build();
  }
}
