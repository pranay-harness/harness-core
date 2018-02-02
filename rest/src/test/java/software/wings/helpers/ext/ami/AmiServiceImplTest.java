package software.wings.helpers.ext.ami;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;

import com.amazonaws.services.ec2.model.Filter;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AmiServiceImplTest {
  @Test
  public void testGetFiltersWithNullEmptyValues() {
    AmiServiceImpl service = new AmiServiceImpl();
    List<Filter> filters = service.getFilters(null, null);
    assertThat(filters.size()).isEqualTo(2);
    assertThat(filters.get(0).getName()).isEqualTo("is-public");
    assertThat(filters.get(0).getValues().size()).isEqualTo(1);
    assertThat(filters.get(0).getValues().get(0)).isEqualTo("false");

    assertThat(filters.get(1).getName()).isEqualTo("state");
    assertThat(filters.get(1).getValues().size()).isEqualTo(1);
    assertThat(filters.get(1).getValues().get(0)).isEqualTo("available");

    filters = service.getFilters(new HashMap<>(), new HashMap<>());
    assertThat(filters.size()).isEqualTo(2);
    assertThat(filters.get(0).getName()).isEqualTo("is-public");
    assertThat(filters.get(0).getValues().size()).isEqualTo(1);
    assertThat(filters.get(0).getValues().get(0)).isEqualTo("false");

    assertThat(filters.get(1).getName()).isEqualTo("state");
    assertThat(filters.get(1).getValues().size()).isEqualTo(1);
    assertThat(filters.get(1).getValues().get(0)).isEqualTo("available");
  }

  @Test
  public void testGetFiltersWithTags() {
    AmiServiceImpl service = new AmiServiceImpl();
    Map<String, List<String>> tagMap = new HashMap<>();
    tagMap.put("tagkey", asList("tagValue1", "tagValue2"));
    List<Filter> filters = service.getFilters(tagMap, new HashMap<>());
    assertThat(filters.size()).isEqualTo(3);
    assertThat(filters.get(0).getName()).isEqualTo("is-public");
    assertThat(filters.get(0).getValues().size()).isEqualTo(1);
    assertThat(filters.get(0).getValues().get(0)).isEqualTo("false");

    assertThat(filters.get(1).getName()).isEqualTo("state");
    assertThat(filters.get(1).getValues().size()).isEqualTo(1);
    assertThat(filters.get(1).getValues().get(0)).isEqualTo("available");

    assertThat(filters.get(2).getName()).isEqualTo("tag:tagkey");
    assertThat(filters.get(2).getValues().size()).isEqualTo(2);
    assertThat(filters.get(2).getValues().get(0)).isEqualTo("tagValue1");
    assertThat(filters.get(2).getValues().get(1)).isEqualTo("tagValue2");
  }

  @Test
  public void testGetFiltersWithFilterMap() {
    AmiServiceImpl service = new AmiServiceImpl();
    Map<String, String> filterMap = new HashMap<>();
    filterMap.put("ami-key1", "value1");
    filterMap.put("ami-key2", "value2");
    filterMap.put("ami-key3", "value3");
    filterMap.put("ami-key4", "");
    filterMap.put("ami-key5", null);
    filterMap.put(null, null);
    filterMap.put(null, "value6");
    filterMap.put("", "value7");
    filterMap.put("wrongkey", "value8");

    List<Filter> filters = service.getFilters(new HashMap<>(), filterMap);

    assertThat(filters.size()).isEqualTo(5);
    assertThat(filters.get(0).getName()).isEqualTo("is-public");
    assertThat(filters.get(0).getValues().size()).isEqualTo(1);
    assertThat(filters.get(0).getValues().get(0)).isEqualTo("false");

    assertThat(filters.get(1).getName()).isEqualTo("state");
    assertThat(filters.get(1).getValues().size()).isEqualTo(1);
    assertThat(filters.get(1).getValues().get(0)).isEqualTo("available");

    assertThat(filters.get(2).getName()).isEqualTo("key1");
    assertThat(filters.get(2).getValues().size()).isEqualTo(1);
    assertThat(filters.get(2).getValues().get(0)).isEqualTo("value1");

    assertThat(filters.get(3).getName()).isEqualTo("key2");
    assertThat(filters.get(3).getValues().size()).isEqualTo(1);
    assertThat(filters.get(3).getValues().get(0)).isEqualTo("value2");

    assertThat(filters.get(4).getName()).isEqualTo("key3");
    assertThat(filters.get(4).getValues().size()).isEqualTo(1);
    assertThat(filters.get(4).getValues().get(0)).isEqualTo("value3");
  }
}
