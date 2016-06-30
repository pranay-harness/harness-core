/**
 *
 */
package software.wings.common;

import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.api.PartitionElement;
import software.wings.beans.ErrorCodes;
import software.wings.exception.WingsException;
import software.wings.sm.ContextElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rishi
 *
 */
public abstract class PartitionProcessor<T extends ContextElement> {
  private static final String PCT = "%";
  private String[] breakdowns;
  private String[] percentages;
  private String[] counts;
  private int minCount = 1;

  public PartitionProcessor withBreakdowns(String... breakdowns) {
    this.breakdowns = breakdowns;
    return this;
  }

  public PartitionProcessor withPercentages(String... percentages) {
    this.percentages = percentages;
    return this;
  }

  public PartitionProcessor withCounts(String... counts) {
    this.counts = counts;
    return this;
  }

  public List<PartitionElement<T>> partitions(String... breakdownsParams) {
    if (ArrayUtils.isNotEmpty(breakdownsParams)) {
      this.breakdowns = breakdownsParams;
    }

    List<T> elements = elements();
    if (elements == null || elements.isEmpty()) {
      return null;
    }
    if ((breakdowns == null || breakdowns.length == 0) && (percentages == null || percentages.length == 0)
        && (counts == null || counts.length == 0)) {
      throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "breakdowns, percentages, counts");
    }
    List<Integer> finalCounts = null;
    try {
      finalCounts = computeCounts(elements.size());
      if (finalCounts == null || finalCounts.isEmpty()) {
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "messages",
            "Incorrect partition breakdown expressions- breakdowns:" + Arrays.toString(breakdowns)
                + "percentages:" + Arrays.toString(percentages) + ", counts:" + Arrays.toString(counts));
      }
    } catch (Exception e) {
      logger.error(e.getMessage(), e);
      throw new WingsException(ErrorCodes.INVALID_REQUEST, "messages",
          "Incorrect partition expressions- breakdowns:" + Arrays.toString(breakdowns)
              + "percentages:" + Arrays.toString(percentages) + ", counts:" + Arrays.toString(counts),
          e);
    }

    List<PartitionElement<T>> partLists = new ArrayList<>();
    int ind = 0;
    for (int count : finalCounts) {
      List<T> elementPart = elements.subList(ind, ind + count);
      ind += count;
      PartitionElement<T> pe = new PartitionElement<>();
      pe.setPartitionElements(elementPart);
      partLists.add(pe);
    }
    return partLists;
  }

  private List<Integer> computeCounts(int total) {
    List<Integer> finalCounts = new ArrayList<>();

    // highest priority to the breakdown
    if (breakdowns != null && breakdowns.length > 0) {
      for (String val : breakdowns) {
        finalCounts.add(pctCountValue(total, val));
      }
      return finalCounts;
    }

    // second priority to the percentages
    if (percentages != null && percentages.length > 0) {
      for (String val : percentages) {
        finalCounts.add(pctCountValue(total, val));
      }
      return finalCounts;
    }
    // second priority to the percentages
    if (counts != null && counts.length > 0) {
      for (String val : percentages) {
        finalCounts.add(pctCountValue(total, val));
      }
      return finalCounts;
    }
    return null;
  }

  private Integer pctCountValue(int total, String val) {
    int count;
    val = val.trim();
    if (val.endsWith(PCT)) {
      count = total * Integer.parseInt(val.substring(0, val.length() - 1).trim()) / 100;
    } else {
      count = Integer.parseInt(val.trim());
    }
    if (count < minCount) {
      count = minCount;
    }
    return count;
  }

  protected abstract List<T> elements();

  private final Logger logger = LoggerFactory.getLogger(getClass());
}
