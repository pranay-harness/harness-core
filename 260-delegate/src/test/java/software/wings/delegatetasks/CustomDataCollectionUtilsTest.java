package software.wings.delegatetasks;

import static io.harness.rule.OwnerRule.SOWMYA;

import static org.assertj.core.api.Assertions.assertThat;

import io.harness.annotations.dev.Module;
import io.harness.annotations.dev.TargetModule;
import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;

import software.wings.WingsBaseTest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@TargetModule(Module._930_DELEGATE_TASKS)
public class CustomDataCollectionUtilsTest extends WingsBaseTest {
  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetMaskedString_whenNoMatchFound() {
    String matchPattern = "apiKey=(.*)";
    String stringToMask = "stringWithoutMatchPattern";

    String maskedString = CustomDataCollectionUtils.getMaskedString(stringToMask, matchPattern, new ArrayList<>());
    assertThat(maskedString).isEqualTo(stringToMask);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetMaskedString_whenExactMatchFound() {
    String matchPattern = "apiKey=(.*)&appKey=(.*)";
    String stringToMask = "stringWithoutMatchPattern&apiKey=encryptedApiKey&appKey=encryptedAppKey";
    List<String> stringsToReplace = Arrays.asList("<apiKey>", "<appKey>");
    String expectedMaskedString = "stringWithoutMatchPattern&apiKey=<apiKey>&appKey=<appKey>";

    String maskedString = CustomDataCollectionUtils.getMaskedString(stringToMask, matchPattern, stringsToReplace);
    assertThat(maskedString).isEqualTo(expectedMaskedString);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetMaskedString_whenStringsToReplaceAreMore() {
    String matchPattern = "apiKey=(.*)&appKey=(.*)";
    String stringToMask = "stringWithoutMatchPattern&apiKey=encryptedApiKey&appKey=encryptedAppKey";
    List<String> stringsToReplace = Arrays.asList("<apiKey>", "<appKey>", "<extraString>");
    String expectedMaskedString = "stringWithoutMatchPattern&apiKey=<apiKey>&appKey=<appKey>";

    String maskedString = CustomDataCollectionUtils.getMaskedString(stringToMask, matchPattern, stringsToReplace);
    assertThat(maskedString).isEqualTo(expectedMaskedString);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testGetMaskedString_whenStringsToReplaceAreLess() {
    String matchPattern = "apiKey=(.*)&appKey=(.*)";
    String stringToMask = "stringWithoutMatchPattern&apiKey=encryptedApiKey&appKey=encryptedAppKey";
    List<String> stringsToReplace = Collections.singletonList("<appKey>");
    String expectedMaskedString = "stringWithoutMatchPattern&apiKey=<appKey>&appKey=encryptedAppKey";

    String maskedString = CustomDataCollectionUtils.getMaskedString(stringToMask, matchPattern, stringsToReplace);
    assertThat(maskedString).isEqualTo(expectedMaskedString);
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveField_emptyString() {
    String string = "";
    String returnValue = CustomDataCollectionUtils.resolveField(string, "%", "test");
    assertThat(returnValue).isEqualTo(string);

    string = null;
    returnValue = CustomDataCollectionUtils.resolveField(string, "%", "test");
    assertThat(returnValue).isNull();
  }

  @Test
  @Owner(developers = SOWMYA)
  @Category(UnitTests.class)
  public void testResolveField_nonEmptyString() {
    String string = "test${replace}";
    String returnValue = CustomDataCollectionUtils.resolveField(string, "${replace}", "replace");
    assertThat(returnValue).isEqualTo("testreplace");
  }
}
