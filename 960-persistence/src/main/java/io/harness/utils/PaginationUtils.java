package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.ng.beans.PageResponse;

import java.util.function.Consumer;
import java.util.function.IntFunction;
import lombok.experimental.UtilityClass;

@UtilityClass
@OwnedBy(PL)
public class PaginationUtils {
  public static <U> void forEachElement(IntFunction<PageResponse<U>> producer, Consumer<U> consumer) {
    int counter = 0;
    PageResponse<U> pageResponse;
    do {
      pageResponse = producer.apply(counter);
      pageResponse.getContent().forEach(consumer);
      counter++;
    } while (pageResponse.getPageItemCount() != 0);
  }
}
