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

import java.util.List;
import java.util.Map;

/**
 * @author Rishi
 *
 */
public class PartitionProcessorTest {
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
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e1);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e2, e3);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e4);
  }

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
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(1).doesNotContainNull().containsExactly(e1);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e2, e3, e4);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e5, e6);
  }

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
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

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
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

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
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

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
        (SamplePartitionProcessor) new SamplePartitionProcessor(sampleElements).withPercentages("2 ", " 3 ", "2");
    List<PartitionElement> partitions = processor.partitions();
    assertThat(partitions).isNotNull().hasSize(3).doesNotContainNull();
    assertThat(partitions.get(0).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e1, e2);
    assertThat(partitions.get(1).getPartitionElements()).hasSize(3).doesNotContainNull().containsExactly(e3, e4, e5);
    assertThat(partitions.get(2).getPartitionElements()).hasSize(2).doesNotContainNull().containsExactly(e6, e7);
  }

  public class SamplePartitionProcessor implements PartitionProcessor {
    private String[] breakdowns;
    private String[] percentages;
    private String[] counts;

    private List<SampleElement> sampleElements;

    public SamplePartitionProcessor(List<SampleElement> sampleElements) {
      super();
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
  public static class SampleElement implements ContextElement {
    private String uuid;

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
    public Map<String, Object> paramMap() {
      return null;
    }
  }
}
