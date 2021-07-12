package types

type Status string
type FileStatus string
type Selection string

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

	// SelectSourceCode represents a selection corresponding to source code changes.
	SelectSourceCode = "source_code"

	// SelectNewTest represents a selection corresponding to a new test (eg a new test
	// introduced in the PR).
	SelectNewTest = "new_test"

	// SelectUpdatedTest represents a selection corresponding to an updated test (eg an existing
	// test which was modified).
	SelectUpdatedTest = "updated_test"

	// SelectFlakyTest represents a selection of a test because it's flaky.
	SelectFlakyTest = "flaky_test"

	// FileModified represents a modified file. Keeping it consistent with git syntax.
	FileModified = "modified"

	// FileAdded represents a file which was added in the PR.
	FileAdded = "added"

	// FileDeleted represents a file which was deleted in the PR.
	FileDeleted = "deleted"
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

type StepInfo struct {
	Step  string `json:"step"`
	Stage string `json:"stage"`
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

// Test Intelligence specific structs

// RunnableTest contains information about a test to run it.
// This is different from TestCase struct which contains information
// about a test case run. RunnableTest is used to run a test.
type RunnableTest struct {
	Pkg       string    `json:"pkg"`
	Class     string    `json:"class"`
	Method    string    `json:"method"`
	Selection Selection `json:"selection"` // information on why a test was selected
}

type SelectTestsResp struct {
	TotalTests    int            `json:"total_tests"`
	SelectedTests int            `json:"selected_tests"`
	NewTests      int            `json:"new_tests"`
	UpdatedTests  int            `json:"updated_tests"`
	SrcCodeTests  int            `json:"src_code_tests"`
	SelectAll     bool           `json:"select_all"` // We might choose to run all the tests
	Tests         []RunnableTest `json:"tests"`
}

type SelectTestsReq struct {
	// If this is specified, TI service will return saying it wants to run all the tests. We want to
	// maintain stats even when all the tests are run.
	SelectAll    bool     `json:"select_all"`
	Files        []File   `json:"files"`
	TargetBranch string   `json:"target_branch"`
	Repo         string   `json:"repo"`
	TiConfig     TiConfig `json:"ti_config"`
}

type SelectionDetails struct {
	New int `json:"new_tests"`
	Upd int `json:"updated_tests"`
	Src int `json:"source_code_changes"`
}

type SelectionOverview struct {
	Total       int              `json:"total_tests"`
	Skipped     int              `json:"skipped_tests"`
	TimeSavedMs int              `json:"time_saved_ms"`
	TimeTakenMs int              `json:"time_taken_ms"`
	Selected    SelectionDetails `json:"selected_tests"`
}

type File struct {
	Name   string     `json:"name"`
	Status FileStatus `json:"status"`
}

// This is a yaml file which may or may not exist in the root of the source code
// as .ticonfig. The contents of the file get deserialized into this object.
// Sample YAML:
// config:
//   ignore:
//     - README.md
//     - config.sh
type TiConfig struct {
	Config struct {
		Ignore []string `json:"ignore"`
	}
}

type DiffInfo struct {
	Sha   string
	Files []File
}

type MergePartialCgRequest struct {
	AccountId    string
	Repo         string
	TargetBranch string
	Diff         DiffInfo
}
