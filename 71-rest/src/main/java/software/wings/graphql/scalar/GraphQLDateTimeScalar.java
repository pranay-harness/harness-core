package software.wings.graphql.scalar;

import graphql.language.StringValue;
import graphql.schema.Coercing;
import graphql.schema.CoercingParseLiteralException;
import graphql.schema.CoercingParseValueException;
import graphql.schema.CoercingSerializeException;
import graphql.schema.GraphQLScalarType;

import java.time.DateTimeException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.function.Function;

/**
 * Created this GraphQLScalar for DateTime scalar.
 * At present adding support for String/Long/Int conversion to date.
 */
public final class GraphQLDateTimeScalar {
  public static String INVALID_INPUT_INSTANCE_TYPE =
      "DateTime scalars needs input to be instanceof OffsetDateTime, ZonedDateTime or String but it was: ";
  public static String INVALID_AST_TYPE = "Expected AST type 'StringValue' but was: ";

  public static final GraphQLScalarType type =
      GraphQLScalarType.newScalar()
          .name("DateTime")
          .description("DateTime Scalar")
          .coercing(new Coercing<OffsetDateTime, String>() {
            @Override
            public String serialize(Object input) throws CoercingSerializeException {
              OffsetDateTime offsetDateTime;
              if (input instanceof OffsetDateTime) {
                offsetDateTime = (OffsetDateTime) input;
              } else if (input instanceof ZonedDateTime) {
                offsetDateTime = ((ZonedDateTime) input).toOffsetDateTime();
              } else if (input instanceof String) {
                offsetDateTime = parseOffsetDateTime(input.toString(), CoercingSerializeException::new);
              } else {
                throw new CoercingSerializeException(INVALID_INPUT_INSTANCE_TYPE + typeName(input));
              }
              return formatDateTime(offsetDateTime);
            }

            @Override
            public OffsetDateTime parseValue(Object input) throws CoercingParseValueException {
              OffsetDateTime offsetDateTime;
              if (input instanceof OffsetDateTime) {
                offsetDateTime = (OffsetDateTime) input;
              } else if (input instanceof ZonedDateTime) {
                offsetDateTime = ((ZonedDateTime) input).toOffsetDateTime();
              } else if (input instanceof String) {
                offsetDateTime = parseOffsetDateTime(input.toString(), CoercingParseValueException::new);
              } else {
                throw new CoercingParseValueException("Expected a 'String' but was '" + typeName(input) + "'.");
              }
              return offsetDateTime;
            }

            @Override
            public OffsetDateTime parseLiteral(Object input) throws CoercingParseLiteralException {
              if (!(input instanceof StringValue)) {
                throw new CoercingParseLiteralException(INVALID_AST_TYPE + typeName(input));
              }
              return parseOffsetDateTime(((StringValue) input).getValue(), CoercingParseLiteralException::new);
            }
          })
          .build();

  public static OffsetDateTime convert(Long timeStamp) {
    OffsetDateTime offsetDateTime = null;
    if (timeStamp != null) {
      offsetDateTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timeStamp), ZoneId.of("UTC"));
    }
    return offsetDateTime;
  }

  public static String formatDateTime(OffsetDateTime offsetDateTime) {
    try {
      return DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(offsetDateTime);
    } catch (DateTimeException e) {
      throw new CoercingSerializeException(
          "Unable to format input argument to OffsetDateTime because of :" + e.getMessage());
    }
  }

  public static String convertToString(Long timeStamp) {
    String offsetDateTimeString = null;
    if (timeStamp != null) {
      offsetDateTimeString = formatDateTime(convert(timeStamp));
    }
    return offsetDateTimeString;
  }

  public static String typeName(Object input) {
    if (input == null) {
      return "null";
    }
    return input.getClass().getSimpleName();
  }

  public static OffsetDateTime parseOffsetDateTime(String s, Function<String, RuntimeException> exceptionMaker) {
    try {
      return OffsetDateTime.parse(s, DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    } catch (DateTimeParseException e) {
      throw exceptionMaker.apply("Invalid RFC3339 value : '" + s + "'. because of : '" + e.getMessage() + "'");
    }
  }
}
