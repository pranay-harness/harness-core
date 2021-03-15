package io.harness.plancreator.steps.approval;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(value =
    { @Type(value = JexlCriteria.class, name = "Jexl")
      , @Type(value = KeyValueCriteria.class, name = "KeyValues") })
public interface Criteria {}
