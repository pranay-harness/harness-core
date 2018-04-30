/**
 *
 */

package software.wings.common;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;

import org.assertj.core.util.Lists;
import org.junit.Test;
import software.wings.api.PartitionElement;
import software.wings.sm.ContextElement;
import software.wings.sm.ContextElementType;
import software.wings.sm.ExecutionContext;

import java.util.List;
import java.util.Map;

/**
 * The type Partition processor test.
 *
 * @author Rishi
 */
public class PartitionProcessorTest {
  /**
   * Should partition by count.
   */
  @Test
  public void shouldPartitionByCount() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5);
    SamplePartitionProcessor processor = new SamplePartitionProcessor(sampleElements);
    List<PartitionElement> partitions = processor.partitions("1", "2", "1");
    assertThat(partitions)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e1);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e2, e3);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e4);
  }

  /**
   * Should partition by pct.
   */
  @Test
  public void shouldPartitionByPct() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7);
    SamplePartitionProcessor processor = new SamplePartitionProcessor(sampleElements);
    List<PartitionElement> partitions = processor.partitions("10 % ", "50 %", "30 %");
    assertThat(partitions)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e1);
    assertThat(partitions.get(1).getPartitionElements())
        .hasSize(4)
        .doesNotContainNull()
        .containsExactly(e2, e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

  /**
   * Should partition by pct with all.
   */
  @Test
  public void shouldPartitionByPctWithAll() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    SampleElement e8 = new SampleElement("e8");
    SampleElement e9 = new SampleElement("e9");
    SampleElement e10 = new SampleElement("e10");
    SampleElement e11 = new SampleElement("e11");
    SampleElement e12 = new SampleElement("e12");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7, e8, e9, e10, e11, e12);
    SamplePartitionProcessor processor = new SamplePartitionProcessor(sampleElements);
    List<PartitionElement> partitions = processor.withPercentages("10 % ", "20 %", "30 %", "40%").partitions();
    assertThat(partitions)
        .isNotNull()
        .hasSize(4)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3", "Phase-4");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements())
        .hasSize(4)
        .doesNotContainNull()
        .containsExactly(e6, e7, e8, e9);
    assertThat(partitions.get(3).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e10, e11, e12);
  }

  /**
   * Should partition by mixed pct and count.
   */
  @Test
  public void shouldPartitionByMixedPctAndCount() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7);
    SamplePartitionProcessor processor = new SamplePartitionProcessor(sampleElements);
    List<PartitionElement> partitions = processor.partitions(" 2 ", "3", "30 %");
    assertThat(partitions)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

  /**
   * Should partition with mixed pct and count.
   */
  @Test
  public void shouldPartitionWithMixedPctAndCount() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7);
    SamplePartitionProcessor processor =
        (SamplePartitionProcessor) new SamplePartitionProcessor(sampleElements).withBreakdowns(" 2 ", "3", "30 %");
    List<PartitionElement> partitions = processor.partitions();
    assertThat(partitions)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

  /**
   * Should partition by with pct.
   */
  @Test
  public void shouldPartitionByWithPct() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7);
    SamplePartitionProcessor processor =
        (SamplePartitionProcessor) new SamplePartitionProcessor(sampleElements).withPercentages("33%", "50%", "30 %");
    List<PartitionElement> partitions = processor.partitions();
    assertThat(partitions)
        .isNotNull()
        .hasSize(2)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e1, e2, e3);
    assertThat(partitions.get(1).getPartitionElements())
        .hasSize(4)
        .doesNotContainNull()
        .containsExactly(e4, e5, e6, e7);
  }

  /**
   * Should partition by with count.
   */
  @Test
  public void shouldPartitionByWithCount() {
    SampleElement e1 = new SampleElement("e1");
    SampleElement e2 = new SampleElement("e2");
    SampleElement e3 = new SampleElement("e3");
    SampleElement e4 = new SampleElement("e4");
    SampleElement e5 = new SampleElement("e5");
    SampleElement e6 = new SampleElement("e6");
    SampleElement e7 = new SampleElement("e7");
    List<SampleElement> sampleElements = Lists.newArrayList(e1, e2, e3, e4, e5, e6, e7);
    SamplePartitionProcessor processor =
        (SamplePartitionProcessor) new SamplePartitionProcessor(sampleElements).withCounts("2 ", " 3 ", "2");
    List<PartitionElement> partitions = processor.partitions();
    assertThat(partitions)
        .isNotNull()
        .hasSize(3)
        .doesNotContainNull()
        .extracting(PartitionElement::getUuid)
        .containsExactly("Phase-1", "Phase-2", "Phase-3");
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

  /**
   * The type Sample element.
   */
  public static class SampleElement implements ContextElement {
    private String uuid;

    /**
     * Instantiates a new Sample element.
     *
     * @param uuid the uuid
     */
    public SampleElement(String uuid) {
      this.uuid = uuid;
    }

    @Override
    public ContextElementType getElementType() {
      return null;
    }

    @Override
    public String getUuid() {
      return uuid;
    }

    @Override
    public String getName() {
      return null;
    }

    @Override
    public Map<String, Object> paramMap(ExecutionContext context) {
      return null;
    }

    @Override
    public ContextElement cloneMin() {
      return this;
    }
  }

  /**
   * The type Sample partition processor.
   */
  public class SamplePartitionProcessor implements PartitionProcessor {
    private String[] breakdowns;
    private String[] percentages;
    private String[] counts;

    private List<SampleElement> sampleElements;

    /**
     * Instantiates a new Sample partition processor.
     *
     * @param sampleElements the sample elements
     */
    public SamplePartitionProcessor(List<SampleElement> sampleElements) {
      this.sampleElements = sampleElements;
    }

    @Override
    public String[] getCounts() {
      return counts;
    }

    @Override
    public void setCounts(String[] counts) {
      this.counts = counts;
    }

    @Override
    public String[] getPercentages() {
      return percentages;
    }

    @Override
    public void setPercentages(String[] percentages) {
      this.percentages = percentages;
    }

    @Override
    public String[] getBreakdowns() {
      return breakdowns;
    }

    @Override
    public void setBreakdowns(String[] breakdowns) {
      this.breakdowns = breakdowns;
    }

    @Override
    public List<ContextElement> elements() {
      return sampleElements.stream().map(sampleElement -> (ContextElement) sampleElement).collect(toList());
    }
  }
}
