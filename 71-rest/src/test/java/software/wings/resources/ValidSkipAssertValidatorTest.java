package software.wings.resources;

import static io.harness.rule.OwnerRule.VGLIJIN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import io.harness.category.element.UnitTests;
import io.harness.rule.Owner;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import javax.validation.ConstraintValidatorContext;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder;
import javax.validation.ConstraintValidatorContext.ConstraintViolationBuilder.NodeBuilderCustomizableContext;

public class ValidSkipAssertValidatorTest {
  private static final ValidSkipAssertValidator validator = new ValidSkipAssertValidator();
  private final ConstraintValidatorContext context = mock(ConstraintValidatorContext.class);

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldReturnFalse() {
    doNothing().when(context).disableDefaultConstraintViolation();
    ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
    doReturn(builder).when(context).buildConstraintViolationWithTemplate(anyString());
    NodeBuilderCustomizableContext nodeBuilderCustomizableContext = mock(NodeBuilderCustomizableContext.class);
    doReturn(nodeBuilderCustomizableContext).when(builder).addPropertyNode(anyString());
    doReturn(context).when(nodeBuilderCustomizableContext).addConstraintViolation();
    assertThat(validator.isValid(",,,", context)).isFalse();
    verify(context).disableDefaultConstraintViolation();
    verify(context).buildConstraintViolationWithTemplate(anyString());
    verify(builder).addPropertyNode(anyString());
    verify(nodeBuilderCustomizableContext).addConstraintViolation();
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldReturnTrue() {
    doNothing().when(context).disableDefaultConstraintViolation();
    ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
    doReturn(builder).when(context).buildConstraintViolationWithTemplate(anyString());
    NodeBuilderCustomizableContext nodeBuilderCustomizableContext = mock(NodeBuilderCustomizableContext.class);
    doReturn(nodeBuilderCustomizableContext).when(builder).addPropertyNode(anyString());
    doReturn(context).when(nodeBuilderCustomizableContext).addConstraintViolation();
    assertThat(validator.isValid("a == b", context)).isTrue();
    verify(context, never()).disableDefaultConstraintViolation();
    verify(context, never()).buildConstraintViolationWithTemplate(anyString());
    verify(builder, never()).addPropertyNode(anyString());
    verify(nodeBuilderCustomizableContext, never()).addConstraintViolation();
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldReturnTrueForExpressions() {
    doNothing().when(context).disableDefaultConstraintViolation();
    assertThat(validator.isValid("${a} == ${b} or ${c} == ${d}", context)).isTrue();
    assertThat(validator.isValid("${a.${b}} == ${b.${a}} and ${c.${a}} == ${d.${b}}", context)).isTrue();
    assertThat(validator.isValid("${a.${b}} =~ {1,2,3}", context)).isTrue();
  }

  @Test
  @Owner(developers = VGLIJIN)
  @Category(UnitTests.class)
  public void shouldReturnFalseForExpressions() {
    doNothing().when(context).disableDefaultConstraintViolation();
    ConstraintViolationBuilder builder = mock(ConstraintViolationBuilder.class);
    doReturn(builder).when(context).buildConstraintViolationWithTemplate(anyString());
    NodeBuilderCustomizableContext nodeBuilderCustomizableContext = mock(NodeBuilderCustomizableContext.class);
    doReturn(nodeBuilderCustomizableContext).when(builder).addPropertyNode(anyString());
    doReturn(context).when(nodeBuilderCustomizableContext).addConstraintViolation();

    assertThat(validator.isValid("${a.${b}} ~= {1,2,3}", context)).isFalse();
    assertThat(validator.isValid("${a.{b}} =~ {1,2,3}", context)).isFalse();
  }
}
