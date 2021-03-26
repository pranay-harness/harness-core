package io.harness.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import io.harness.beans.SortOrder;
import io.harness.ng.beans.PageRequest;
import io.harness.ng.beans.PageResponse;

import com.google.common.base.Preconditions;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@UtilityClass
public class PageUtils {
  private final String COMMA_SEPARATOR = ",";

  public static Pageable getPageRequest(int page, int size, List<String> sort) {
    if (isEmpty(sort)) {
      return org.springframework.data.domain.PageRequest.of(page, size);
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

    return org.springframework.data.domain.PageRequest.of(page, size, Sort.by(orders));
  }

  public static Pageable getPageRequest(PageRequest pageRequestDTO) {
    List<String> sortOrders = new ArrayList<>();
    if (pageRequestDTO.getSortOrders() != null) {
      for (SortOrder sortOrder : pageRequestDTO.getSortOrders()) {
        sortOrders.add(sortOrder.getFieldName() + COMMA_SEPARATOR + sortOrder.getOrderType());
      }
    }
    return getPageRequest(pageRequestDTO.getPageIndex(), pageRequestDTO.getPageSize(), sortOrders);
  }

  public <T> PageResponse<T> getNGPageResponse(Page<T> page) {
    return PageResponse.<T>builder()
        .totalPages(page.getTotalPages())
        .totalItems(page.getTotalElements())
        .pageItemCount(page.getContent().size())
        .content(page.getContent())
        .pageSize(page.getSize())
        .pageIndex(page.getNumber())
        .empty(page.isEmpty())
        .build();
  }

  public <T, C> PageResponse<C> getNGPageResponse(Page<T> page, List<C> content) {
    return PageResponse.<C>builder()
        .totalPages(page.getTotalPages())
        .totalItems(page.getTotalElements())
        .pageItemCount((content != null) ? content.size() : 0)
        .content(content)
        .pageSize(page.getSize())
        .pageIndex(page.getNumber())
        .empty(page.isEmpty())
        .build();
  }

  public <T, C> PageResponse<C> getNGPageResponse(PageResponse<T> page, List<C> content) {
    return PageResponse.<C>builder()
        .totalPages(page.getTotalPages())
        .totalItems(page.getTotalItems())
        .pageItemCount((content != null) ? content.size() : 0)
        .content(content)
        .pageSize(page.getPageSize())
        .pageIndex(page.getPageIndex())
        .empty(page.isEmpty())
        .build();
  }

  public <T> PageResponse<T> getNGPageResponse(io.harness.beans.PageResponse<T> page) {
    return PageResponse.<T>builder()
        .totalPages((page.getPageSize() == 0) ? 0 : (page.getTotal() + page.getPageSize() - 1) / page.getPageSize())
        .totalItems(page.getTotal())
        .pageItemCount((page.getResponse() != null) ? page.getResponse().size() : 0)
        .content(page.getResponse())
        .pageSize(page.getPageSize())
        .pageIndex(page.getStart())
        .empty(page.isEmpty())
        .build();
  }

  public <T> PageResponse<T> offsetAndLimit(List<T> input, int offset, int pageSize) {
    Preconditions.checkState(input.size() >= offset * pageSize,
        "for a list of size %s the offset %s and pagesize %s is invalid", input.size(), offset, pageSize);

    int startIndex = offset * pageSize;
    int endIndex = startIndex + pageSize < input.size() ? startIndex + pageSize : input.size();
    int totalPages = (input.size() / pageSize) + (input.size() % pageSize == 0 ? 0 : 1);
    List<T> returnList = input.subList(startIndex, endIndex);
    return PageResponse.<T>builder()
        .pageSize(pageSize)
        .pageIndex(offset)
        .totalPages(totalPages)
        .totalItems(input.size())
        .pageItemCount(returnList.size())
        .content(returnList)
        .build();
  }
}
