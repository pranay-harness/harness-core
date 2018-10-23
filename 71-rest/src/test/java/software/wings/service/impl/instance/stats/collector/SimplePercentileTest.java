package software.wings.service.impl.instance.stats.collector;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class SimplePercentileTest {
  @Test
  public void testPercentile() {
    List<Integer> numbers = new ArrayList<>(100);
    IntStream.range(1, 101).forEach(numbers::add);
    Collections.shuffle(numbers);

    Integer percentile = new SimplePercentile(numbers).evaluate(95);
    assertEquals(sorted(numbers).get(95), percentile);

    percentile = new SimplePercentile(numbers).evaluate(20);
    assertEquals(sorted(numbers).get(20), percentile);

    numbers = new ArrayList<>(20);
    IntStream.range(1, 21).forEach(numbers::add);
    Collections.shuffle(numbers);
    percentile = new SimplePercentile(numbers).evaluate(95);
    assertEquals(sorted(numbers).get(19), percentile);
  }

  private static List<Integer> sorted(List<Integer> list) {
    return list.stream().sorted().collect(Collectors.toList());
  }
}
