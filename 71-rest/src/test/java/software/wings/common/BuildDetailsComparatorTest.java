package software.wings.common;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import io.harness.category.element.UnitTests;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import software.wings.helpers.ext.jenkins.BuildDetails;

import java.util.List;

public class BuildDetailsComparatorTest {
  private BuildDetails.Builder buildDetails = BuildDetails.Builder.aBuildDetails();

  @Test
  @Category(UnitTests.class)
  public void shouldSortDescendingOrder() {
    buildDetails.withNumber("todolist-1.0-1.x86_64.rpm").build();
    List<BuildDetails> buildDetailsList = asList(buildDetails.withNumber("todolist-1.0-1.x86_64.rpm").build(),
        buildDetails.withNumber("todolist-1.0-10.x86_64.rpm").build(),
        buildDetails.withNumber("todolist-1.0-5.x86_64.rpm").build(),
        buildDetails.withNumber("todolist-1.0-15.x86_64.rpm").build());

    assertThat(buildDetailsList.stream().sorted(new BuildDetailsComparator()).collect(toList()))
        .hasSize(4)
        .extracting(BuildDetails::getNumber)
        .containsSequence("todolist-1.0-15.x86_64.rpm", "todolist-1.0-10.x86_64.rpm", "todolist-1.0-5.x86_64.rpm",
            "todolist-1.0-1.x86_64.rpm");
  }
}