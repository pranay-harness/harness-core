/*
 * Copyright 2021 Harness Inc.
 * 
 * Licensed under the Apache License, Version 2.0
 * http://www.apache.org/licenses/LICENSE-2.0
 */

package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.IntFunction;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class PaginationUtils {
  public static <T> void forEachElement(IntFunction<PageResponse<T>> producer, Consumer<T> consumer) {
    int counter = 0;
    PageResponse<T> pageResponse;
    do {
      pageResponse = producer.apply(counter);
      pageResponse.getContent().forEach(consumer);
      counter++;
    } while (pageResponse.getPageItemCount() != 0);
  }

  public static <T> PageResponse<T> getPage(List<T> content, PageRequest pageRequest) {
    if (Objects.isNull(pageRequest) || Objects.isNull(content) || pageRequest.getPageSize() == 0) {
      return PageResponse.getEmptyPageResponse(pageRequest);
    }
    long lowIdx = (long) pageRequest.getPageIndex() * pageRequest.getPageSize();
    long highIdx = Math.min(lowIdx + pageRequest.getPageSize(), content.size());
    if (lowIdx < 0 || lowIdx >= content.size()) {
      return PageResponse.getEmptyPageResponse(pageRequest);
    }
    return PageResponse.<T>builder()
        .totalPages((long) Math.ceil((double) content.size() / pageRequest.getPageSize()))
        .totalItems(content.size())
        .pageItemCount(highIdx - lowIdx)
        .content(new ArrayList<>(content.subList((int) lowIdx, (int) highIdx)))
        .pageSize(pageRequest.getPageSize())
        .pageIndex(pageRequest.getPageIndex())
        .empty(false)
        .build();
  }
}
