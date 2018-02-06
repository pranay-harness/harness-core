package software.wings.common;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import java.util.List;
import java.util.stream.Collectors;

public class AlphanumComparatorTest {
  @Test
  public void shouldSortAscendingOrder() {
    List<String> values = asList("todolist-1.0-10.x86_64.rpm", "todolist-1.0-1.x86_64.rpm", "todolist-1.0-3.x86_64.rpm",
        "todolist-1.0-2.x86_64.rpm");
    values = values.stream().sorted(new AlphanumComparator()).collect(Collectors.toList());

    assertThat(values).hasSize(4).containsSequence("todolist-1.0-1.x86_64.rpm", "todolist-1.0-2.x86_64.rpm",
        "todolist-1.0-3.x86_64.rpm", "todolist-1.0-10.x86_64.rpm");
  }
}
