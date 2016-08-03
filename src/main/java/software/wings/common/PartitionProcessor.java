/**
 *
 */

package software.wings.common;

import static org.eclipse.jetty.util.LazyList.isEmpty;

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
 * The interface Partition processor.
 *
 * @author Rishi
 */
public interface PartitionProcessor {
  /**
   * The constant PCT.
   */
  String PCT = "%";
  /**
   * The constant minCount.
   */
  int minCount = 1;

  /**
   * With breakdowns partition processor.
   *
   * @param breakdowns the breakdowns
   * @return the partition processor
   */
  default PartitionProcessor
    withBreakdowns(String... breakdowns) {
      setBreakdowns(breakdowns);
      return this;
    }

  /**
   * With percentages partition processor.
   *
   * @param percentages the percentages
   * @return the partition processor
   */
  default PartitionProcessor
    withPercentages(String... percentages) {
      setPercentages(percentages);
      return this;
    }

  /**
   * With counts partition processor.
   *
   * @param counts the counts
   * @return the partition processor
   */
  default PartitionProcessor
    withCounts(String... counts) {
      setCounts(counts);
      return this;
    }

  /**
   * Partitions list.
   *
   * @param breakdownsParams the breakdowns params
   * @return the list
   */
  default List
    <PartitionElement> partitions(String... breakdownsParams) {
      if (ArrayUtils.isNotEmpty(breakdownsParams)) {
        setBreakdowns(breakdownsParams);
      }

      List<ContextElement> elements = elements();
      if (isEmpty(elements)) {
        return null;
      }

      String[] breakdowns = getBreakdowns();
      String[] percentages = getPercentages();
      String[] counts = getCounts();
      if (isEmpty(breakdowns) && isEmpty(percentages) && isEmpty(counts)) {
        throw new WingsException(ErrorCodes.INVALID_ARGUMENT, "args", "breakdowns, percentages, counts");
      }
      List<Integer> finalCounts = null;
      try {
        finalCounts = computeCounts(elements.size());
        if (isEmpty(finalCounts)) {
          throw new WingsException(ErrorCodes.INVALID_REQUEST, "messages",
              "Incorrect partition breakdown expressions- breakdowns:" + Arrays.toString(breakdowns)
                  + "percentages:" + Arrays.toString(percentages) + ", counts:" + Arrays.toString(counts));
        }
      } catch (Exception e) {
        log().error(e.getMessage(), e);
        throw new WingsException(ErrorCodes.INVALID_REQUEST, "messages",
            "Incorrect partition expressions- breakdowns:" + Arrays.toString(breakdowns)
                + "percentages:" + Arrays.toString(percentages) + ", counts:" + Arrays.toString(counts),
            e);
      }

      List<PartitionElement> partLists = new ArrayList<>();
      int ind = 0;
      int partitionIndex = 1;
      for (int count : finalCounts) {
        if (ind < elements.size()) {
          List<ContextElement> elementPart =
              new ArrayList<>(elements.subList(ind, Math.min(ind + count, elements.size())));
          ind += count;
          PartitionElement pe = new PartitionElement();
          pe.setPartitionElements(elementPart);
          pe.setName("Phase " + partitionIndex);
          partitionIndex++;
          partLists.add(pe);
        }
      }
      return partLists;
    }

  /**
   * Compute counts list.
   *
   * @param total the total
   * @return the list
   */
  default List
    <Integer> computeCounts(int total) {
      String[] breakdowns = getBreakdowns();
      String[] percentages = getPercentages();
      String[] counts = getCounts();

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

    /**
     * Getter for property 'counts'.
     *
     * @return Value for property 'counts'.
     */
    String[] getCounts();

    /**
     * Sets counts.
     *
     * @param counts the counts
     */
    void setCounts(String[] counts);

    /**
     * Getter for property 'percentages'.
     *
     * @return Value for property 'percentages'.
     */
    String[] getPercentages();

    /**
     * Sets percentages.
     *
     * @param percentages the percentages
     */
    void setPercentages(String[] percentages);

    /**
     * Getter for property 'breakdowns'.
     *
     * @return Value for property 'breakdowns'.
     */
    String[] getBreakdowns();

    /**
     * Setter for property 'breakdowns'.
     *
     * @param breakdowns Value to set for property 'breakdowns'.
     */
    void setBreakdowns(String[] breakdowns);

  /**
   * Pct count value integer.
   *
   * @param total the total
   * @param val   the val
   * @return the integer
   */
  default Integer
    pctCountValue(int total, String val) {
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

    /**
     * Elements list.
     *
     * @return the list
     */
    List<ContextElement> elements();

  /**
   * Log logger.
   *
   * @return the logger
   */
  default Logger
    log() {
      return LoggerFactory.getLogger(getClass());
    }
}
