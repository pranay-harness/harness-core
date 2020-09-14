package io.harness.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.NGPageRequest;
import io.harness.beans.NGPageResponse;
import io.harness.beans.PageResponse;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class PageUtils {
  private final String COMMA_SEPARATOR = ",";

  public static Pageable getPageRequest(int page, int size, List<String> sort) {
    if (isEmpty(sort)) {
      return PageRequest.of(page, size);
    }

    List<Sort.Order> orders = new ArrayList<>();
    for (String propOrder : sort) {
      String[] propOrderSplit = propOrder.split(COMMA_SEPARATOR);
      String property = propOrderSplit[0];
      if (propOrderSplit.length == 1) {
        orders.add(new Sort.Order(Sort.Direction.ASC, property));
      } else {
        Sort.Direction direction = Sort.Direction.fromString(propOrderSplit[1]);
        orders.add(new Sort.Order(direction, property));
      }
    }

    return PageRequest.of(page, size, Sort.by(orders));
  }

  public static Pageable getPageRequest(NGPageRequest pageRequestDTO) {
    return getPageRequest(pageRequestDTO.getPage(), pageRequestDTO.getSize(), pageRequestDTO.getSort());
  }

  public <T> NGPageResponse<T> getNGPageResponse(Page<T> page) {
    return NGPageResponse.<T>builder()
        .pageCount(page.getTotalPages())
        .itemCount(page.getTotalElements())
        .content(page.getContent())
        .pageSize(page.getSize())
        .pageIndex(page.getPageable().getPageNumber())
        .empty(page.isEmpty())
        .build();
  }

  public <T, C> NGPageResponse<C> getNGPageResponse(Page<T> page, List<C> content) {
    return NGPageResponse.<C>builder()
        .pageCount(page.getTotalPages())
        .itemCount(page.getTotalElements())
        .content(content)
        .pageSize(page.getSize())
        .pageIndex(page.getPageable().getPageNumber())
        .empty(page.isEmpty())
        .build();
  }

  public <T> NGPageResponse<T> getNGPageResponse(PageResponse<T> page) {
    return NGPageResponse.<T>builder()
        .pageCount((page.getPageSize() == 0) ? 0 : (page.getTotal() + page.getPageSize() - 1) / page.getPageSize())
        .itemCount(page.getTotal())
        .content(page.getResponse())
        .pageSize(page.getPageSize())
        .pageIndex(page.getStart())
        .empty(page.isEmpty())
        .build();
  }
}
