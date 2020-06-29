package io.harness.utils;

import static io.harness.data.structure.EmptyPredicate.isEmpty;

import lombok.experimental.UtilityClass;
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
}
