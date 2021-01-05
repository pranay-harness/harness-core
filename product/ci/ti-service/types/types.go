package types

type Status string

const (
	// StatusPassed represents a passed test.
	StatusPassed = "passed"

	// StatusSkipped represents a test case that was intentionally skipped.
	StatusSkipped = "skipped"

	// StatusFailure represents a violation of declared test expectations,
	// such as a failed assertion.
	StatusFailed = "failed"

	// StatusError represents an unexpected violation of the test itself, such as
	// an uncaught exception.
	StatusError = "error"
)

type Result struct {
	Status  Status `json:"status"`
	Message string `json:"message"`
	Type    string `json:"type"`
	Desc    string `json:"desc"`
}

type ResponseMetadata struct {
	TotalPages    int `json:"totalPages"`
	TotalItems    int `json:"totalItems"`
	PageItemCount int `json:"pageItemCount"`
	PageSize      int `json:"pageSize"`
}

type TestCases struct {
	Metadata ResponseMetadata `json:"data"`
	Tests    []TestCase       `json:"content"`
}

type TestSuites struct {
	Metadata ResponseMetadata `json:"data"`
	Suites   []TestSuite      `json:"content"`
}

type TestCase struct {
	Name       string `json:"name"`
	ClassName  string `json:"class_name"`
	SuiteName  string `json:"suite_name"`
	Result     Result `json:"result"`
	DurationMs int64  `json:"duration_ms"`
	SystemOut  string `json:"stdout"`
	SystemErr  string `json:"stderr"`
}

type TestSummary struct {
	Name   string `json:"name"`
	Status Status `json:"status"`
}

type SummaryResponse struct {
	TotalTests int           `json:"total_tests"`
	TimeMs     int64         `json:"duration_ms"`
	Tests      []TestSummary `json:"tests"`
}

type TestSuite struct {
	Name         string `json:"name"`
	DurationMs   int64  `json:"duration_ms"`
	TotalTests   int    `json:"total_tests"`
	FailedTests  int    `json:"failed_tests"`
	SkippedTests int    `json:"skipped_tests"`
	PassedTests  int    `json:"passed_tests"`
	FailPct      int    `json:"fail_pct"`
}
