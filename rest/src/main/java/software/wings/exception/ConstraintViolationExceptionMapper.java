package software.wings.exception;

import static io.harness.eraro.ErrorCode.INVALID_ARGUMENT;
import static java.util.stream.Collectors.toList;
import static software.wings.beans.ResponseMessage.aResponseMessage;
import static software.wings.beans.RestResponse.Builder.aRestResponse;

import com.google.common.base.Joiner;
import com.google.common.base.Strings;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.dropwizard.validation.ValidationMethod;
import io.harness.eraro.Level;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.commons.lang3.reflect.MethodUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.wings.beans.ResponseMessage;
import software.wings.beans.RestResponse;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.ElementKind;
import javax.validation.Path;
import javax.validation.metadata.ConstraintDescriptor;
import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

/**
 * Created by peeyushaggarwal on 6/8/16.
 */
@Provider
public class ConstraintViolationExceptionMapper implements ExceptionMapper<ConstraintViolationException> {
  private static final Logger logger = LoggerFactory.getLogger(ConstraintViolationExceptionMapper.class);

  private static final Cache<Pair<Path, ? extends ConstraintDescriptor<?>>, String> MESSAGES_CACHE =
      CacheBuilder.newBuilder().expireAfterWrite(1, TimeUnit.HOURS).build();
  private static final Joiner DOT_JOINER = Joiner.on('.');

  private static String getMessage(ConstraintViolation<?> v) {
    Pair<Path, ? extends ConstraintDescriptor<?>> of = Pair.of(v.getPropertyPath(), v.getConstraintDescriptor());

    String message = MESSAGES_CACHE.getIfPresent(of);
    if (message == null) {
      message = calculateMessage(v);
      MESSAGES_CACHE.put(of, message);
    }
    return message;
  }

  private static String calculateMessage(ConstraintViolation<?> v) {
    final Optional<String> returnValueName = getMethodReturnValueName(v);
    if (returnValueName.isPresent()) {
      final String name =
          isValidationMethod(v) ? StringUtils.substringBeforeLast(returnValueName.get(), ".") : returnValueName.get();
      return name + " " + v.getMessage();
    } else if (isValidationMethod(v)) {
      return validationMethodFormatted(v);
    } else {
      final String name = getMemberName(v).orElse(v.getPropertyPath().toString());
      return name + " " + v.getMessage();
    }
  }

  private static Optional<String> getMethodReturnValueName(ConstraintViolation<?> violation) {
    int returnValueNames = -1;

    final StringBuilder result = new StringBuilder("server response");
    for (Path.Node node : violation.getPropertyPath()) {
      if (node.getKind().equals(ElementKind.RETURN_VALUE)) {
        returnValueNames = 0;
      } else if (returnValueNames >= 0) {
        result.append(returnValueNames++ == 0 ? " " : ".").append(node);
      }
    }

    return returnValueNames >= 0 ? Optional.of(result.toString()) : Optional.empty();
  }

  /**
   * Derives member's name and type from it's annotations
   */
  private static Optional<String> getMemberName(Annotation[] memberAnnotations) {
    for (Annotation a : memberAnnotations) {
      if (a instanceof QueryParam) {
        return Optional.of("query param " + ((QueryParam) a).value());
      } else if (a instanceof PathParam) {
        return Optional.of("path param " + ((PathParam) a).value());
      } else if (a instanceof HeaderParam) {
        return Optional.of("header " + ((HeaderParam) a).value());
      } else if (a instanceof CookieParam) {
        return Optional.of("cookie " + ((CookieParam) a).value());
      } else if (a instanceof FormParam) {
        return Optional.of("form field " + ((FormParam) a).value());
      } else if (a instanceof Context) {
        return Optional.of("context");
      } else if (a instanceof MatrixParam) {
        return Optional.of("matrix param " + ((MatrixParam) a).value());
      }
    }

    return Optional.empty();
  }

  /**
   * Gets a method parameter (or a parameter field) name, if the violation raised in it.
   */
  private static Optional<String> getMemberName(ConstraintViolation<?> violation) {
    final int size = Iterables.size(violation.getPropertyPath());
    if (size < 2) {
      return Optional.empty();
    }

    final Path.Node parent = Iterables.get(violation.getPropertyPath(), size - 2);
    final Path.Node member = Iterables.getLast(violation.getPropertyPath());
    final Class<?> resourceClass = violation.getLeafBean().getClass();
    switch (parent.getKind()) {
      case PARAMETER:
        Field field = FieldUtils.getField(resourceClass, member.getName(), true);
        return getMemberName(field.getDeclaredAnnotations());
      case METHOD:
        List<Class<?>> params = parent.as(Path.MethodNode.class).getParameterTypes();
        Class<?>[] parcs = params.toArray(new Class<?>[ 0 ]);
        Method method = MethodUtils.getAccessibleMethod(resourceClass, parent.getName(), parcs);

        int paramIndex = member.as(Path.ParameterNode.class).getParameterIndex();
        return getMemberName(method.getParameterAnnotations()[paramIndex]);
      default:
        return Optional.empty();
    }
  }

  private static boolean isValidationMethod(ConstraintViolation<?> v) {
    return v.getConstraintDescriptor().getAnnotation() instanceof ValidationMethod;
  }

  private static <T> String validationMethodFormatted(ConstraintViolation<T> v) {
    final ImmutableList<Path.Node> nodes = ImmutableList.copyOf(v.getPropertyPath());
    String usefulNodes = DOT_JOINER.join(nodes.subList(0, nodes.size() - 1));
    String msg = usefulNodes + (v.getMessage().charAt(0) == '.' ? "" : " ") + v.getMessage();
    return msg.trim();
  }

  @Override
  public Response toResponse(ConstraintViolationException exception) {
    ImmutableList<String> errors = ImmutableList.copyOf(
        exception.getConstraintViolations().stream().map(v -> processMessage(v, getMessage(v))).collect(toList()));

    if (errors.isEmpty()) {
      errors = ImmutableList.of(Strings.nullToEmpty(exception.getMessage()));
    }

    logger.info(toRestResponse(errors).toString());

    return Response.status(Status.BAD_REQUEST).entity(toRestResponse(errors)).build();
  }

  private RestResponse toRestResponse(ImmutableList<String> errors) {
    return aRestResponse()
        .withResponseMessages(errors.stream().map(this ::errorMessageToResponseMessage).collect(toList()))
        .build();
  }

  private ResponseMessage errorMessageToResponseMessage(String s) {
    return aResponseMessage().code(INVALID_ARGUMENT).level(Level.ERROR).message(s).build();
  }

  @SuppressFBWarnings("DLS_DEAD_LOCAL_STORE")
  private String processMessage(ConstraintViolation<?> v, String message) {
    ConstraintDescriptor constraintDescriptor = v.getConstraintDescriptor();
    boolean replaceArg = false;
    for (Path.Node node : v.getPropertyPath()) {
      switch (node.getKind()) {
        case METHOD: {
          message = message.replace(node + ".", "");
          replaceArg = true;
          break;
        }
        case PARAMETER: {
          if (replaceArg) {
            message = message.replace(node + ".", "");
          }
          break;
        }
        case PROPERTY: {
          if (replaceArg) {
            message = message.replace(node + ".", "");
          }
          break;
        }
        default: { break; }
      }
    }
    return message;
  }
}
