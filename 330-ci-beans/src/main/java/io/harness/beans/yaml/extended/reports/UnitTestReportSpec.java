package io.harness.beans.yaml.extended.reports;

import com.fasterxml.jackson.annotation.JsonSubTypes;

@JsonSubTypes({ @JsonSubTypes.Type(value = JUnitTestReport.class, name = "JUnit") })
public interface UnitTestReportSpec {}
