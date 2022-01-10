/*
 * Copyright 2020 Harness Inc. All rights reserved.
 * Use of this source code is governed by the PolyForm Free Trial 1.0.0 license
 * that can be found in the licenses directory at the root of this repository, also available at
 * https://polyformproject.org/wp-content/uploads/2020/05/PolyForm-Free-Trial-1.0.0.txt.
 */

/**
 *
 */

package software.wings.common;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;

import static java.util.Arrays.asList;

import io.harness.eraro.ErrorCode;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.WingsException;

import software.wings.api.PartitionElement;
import software.wings.sm.ContextElement;

import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;

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
  default PartitionProcessor withBreakdowns(String... breakdowns) {
    setBreakdowns(asList(breakdowns));
    return this;
  }

  /**
   * With percentages partition processor.
   *
   * @param percentages the percentages
   * @return the partition processor
   */
  default PartitionProcessor withPercentages(String... percentages) {
    setPercentages(asList(percentages));
    return this;
  }

  /**
   * With counts partition processor.
   *
   * @param counts the counts
   * @return the partition processor
   */
  default PartitionProcessor withCounts(String... counts) {
    setCounts(asList(counts));
    return this;
  }

  /**
   * Partitions list.
   *
   * @param breakdownsParams the breakdowns params
   * @return the list
   */
  default List<PartitionElement> partitions(Logger logger, String... breakdownsParams) {
    if (isNotEmpty(breakdownsParams)) {
      setBreakdowns(asList(breakdownsParams));
    }

    List<ContextElement> elements = elements();
    if (isEmpty(elements)) {
      return null;
    }

    List<String> breakdowns = getBreakdowns();
    List<String> percentages = getPercentages();
    List<String> counts = getCounts();
    if (isEmpty(breakdowns) && isEmpty(percentages) && isEmpty(counts)) {
      throw new WingsException(ErrorCode.INVALID_ARGUMENT).addParam("args", "breakdowns, percentages, counts");
    }
    List<Integer> finalCounts = null;
    try {
      finalCounts = computeCounts(elements.size());
      if (isEmpty(finalCounts)) {
        throw new InvalidRequestException("Incorrect partition breakdown expressions- breakdowns:"
            + breakdowns.toString() + "percentages:" + percentages.toString() + ", counts:" + counts.toString());
      }
    } catch (Exception e) {
      throw new InvalidRequestException("Incorrect partition expressions- breakdowns:" + breakdowns.toString()
              + "percentages:" + percentages.toString() + ", counts:" + counts.toString(),
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
        PartitionElement pe = PartitionElement.builder()
                                  .partitionElements(elementPart)
                                  .name("Phase " + partitionIndex)
                                  .uuid("Phase-" + partitionIndex)
                                  .build();
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
  default List<Integer> computeCounts(int total) {
    List<String> breakdowns = getBreakdowns();
    List<String> percentages = getPercentages();
    List<String> counts = getCounts();

    List<Integer> finalCounts = new ArrayList<>();

    // highest priority to the breakdown
    if (isNotEmpty(breakdowns)) {
      for (String val : breakdowns) {
        finalCounts.add(pctCountValue(total, val));
      }
      return finalCounts;
    }

    // second priority to the percentages
    if (isNotEmpty(percentages)) {
      for (String val : percentages) {
        finalCounts.add(pctCountValue(total, val));
      }
      return finalCounts;
    }
    // second priority to the percentages
    if (isNotEmpty(counts)) {
      for (String val : counts) {
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
  List<String> getCounts();

  /**
   * Sets counts.
   *
   * @param counts the counts
   */
  void setCounts(List<String> counts);

  /**
   * Getter for property 'percentages'.
   *
   * @return Value for property 'percentages'.
   */
  List<String> getPercentages();

  /**
   * Sets percentages.
   *
   * @param percentages the percentages
   */
  void setPercentages(List<String> percentages);

  /**
   * Getter for property 'breakdowns'.
   *
   * @return Value for property 'breakdowns'.
   */
  List<String> getBreakdowns();

  /**
   * Setter for property 'breakdowns'.
   *
   * @param breakdowns Value to set for property 'breakdowns'.
   */
  void setBreakdowns(List<String> breakdowns);

  /**
   * Pct count value integer.
   *
   * @param total the total
   * @param val   the val
   * @return the integer
   */
  default Integer pctCountValue(int total, String val) {
    int count;
    val = val.trim();
    if (val.endsWith(PCT)) {
      count = (int) Math.ceil((total * Integer.parseInt(val.substring(0, val.length() - 1).trim())) / 100.0);
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
}
