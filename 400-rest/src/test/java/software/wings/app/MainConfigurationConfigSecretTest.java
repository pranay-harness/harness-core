package software.wings.app;

import static io.harness.rule.OwnerRule.BOGDAN;

import static org.assertj.core.api.Assertions.fail;

import io.harness.annotations.dev.HarnessTeam;
import io.harness.annotations.dev.OwnedBy;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import io.harness.secret.ConfigSecret;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Slf4j
@OwnedBy(HarnessTeam.PL)
public class MainConfigurationConfigSecretTest {
  @Test
  @Owner(developers = BOGDAN)
  @Category(UnitTests.class)
  public void configSecretsShouldHaveAllParentsMarkWithConfigSecretAnnotation() {
    List<List<String>> fails = Lists.newArrayList();
    for (Field field : FieldUtils.getFieldsListWithAnnotation(MainConfiguration.class, JsonProperty.class)) {
      boolean isSecret = field.isAnnotationPresent(ConfigSecret.class);

      if (couldBeConfigClass(field)) {
        fails.addAll(traverse(field, !isSecret, ImmutableList.of(MainConfiguration.class.getName(), field.getName())));
      }
    }

    if (!fails.isEmpty()) {
      StringBuilder errorMessageBuilder = new StringBuilder("Following paths failed: \n ");
      for (List<String> failElement : fails) {
        errorMessageBuilder.append("  ").append(failElement).append(System.lineSeparator());
      }
      String errorMessage = errorMessageBuilder.toString();
      fail(errorMessage);
    }
  }

  private List<List<String>> traverse(Field field, boolean pathLacksMarker, List<String> pathRoute) {
    List<List<String>> currentFails = new ArrayList<>();
    for (Field subfield : FieldUtils.getAllFields(field.getType())) {
      boolean subfieldMarked = subfield.isAnnotationPresent(ConfigSecret.class);

      if (pathLacksMarker && subfieldMarked) {
        List<String> failPath = Lists.newArrayList(pathRoute);
        failPath.add(subfield.getName());
        currentFails.add(failPath);
      }

      if (couldBeConfigClass(subfield)) {
        boolean subPathLacksMarker = pathLacksMarker || !subfieldMarked;
        List<String> newParentTypes = Lists.newArrayList(pathRoute);
        newParentTypes.add(subfield.getType().getTypeName());
        currentFails.addAll(traverse(subfield, subPathLacksMarker, newParentTypes));
      }
    }
    return currentFails;
  }

  private boolean couldBeConfigClass(Field field) {
    return !field.getType().isEnum()
        && (field.getType().getName().startsWith("io.harness")
            || field.getType().getName().startsWith("software.wings"));
  }
}