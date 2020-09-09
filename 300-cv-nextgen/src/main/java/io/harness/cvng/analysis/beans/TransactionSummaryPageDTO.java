package io.harness.cvng.analysis.beans;

import lombok.Builder;
import lombok.Value;

import java.util.Collections;
import java.util.List;

@Value
@Builder
public class TransactionSummaryPageDTO {
  private List<DeploymentTimeSeriesAnalysisDTO.TransactionSummary> transactionSummaries;
  private int numberOfPages;
  private int pageNumber;
  private ElementRange elementRange;

  @Value
  @Builder
  public static class ElementRange {
    int fromIndex;
    int toIndex;
  }
  /**
   * returns a view (not a new list) of the sourceList for the
   * range based on pageNumber and pageSize
   * @param sourceList
   * @param pageNumber
   * @param pageSize
   * @return
   */
  public static <T> List<T> getPage(List<T> sourceList, int pageNumber, int pageSize) {
    int fromIndex = (pageNumber - 1) * pageSize;
    if (sourceList == null || sourceList.size() < fromIndex) {
      return Collections.emptyList();
    }
    int toIndex = Math.min(fromIndex + pageSize, sourceList.size());
    return sourceList.subList(fromIndex, toIndex);
  }

  public static <T> ElementRange setElementRange(List<T> sourceList, int pageNumber, int pageSize) {
    int fromIndex = (pageNumber - 1) * pageSize;
    if (sourceList == null || sourceList.size() < fromIndex) {
      return ElementRange.builder().fromIndex(0).toIndex(0).build();
    }
    int toIndex = Math.min(fromIndex + pageSize, sourceList.size());
    return ElementRange.builder().fromIndex(sourceList.size() > 0 ? fromIndex + 1 : 0).toIndex(toIndex).build();
  }

  public static int setNumberOfPages(int listSize, int pageSize) {
    int numberOfPages = listSize / pageSize;
    if (numberOfPages == 0) {
      return 1;
    } else if (listSize % pageSize != 0) {
      return numberOfPages + 1;
    }
    return numberOfPages;
  }
}
