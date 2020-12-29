package timescaledb

import (
	"context"
	"errors"
	"fmt"
	"github.com/hashicorp/go-multierror"
	"github.com/wings-software/portal/commons/go/lib/db"
	"go.uber.org/zap"
	"strconv"

	"github.com/wings-software/portal/product/ci/ti-service/types"
)

const (
	defaultOffset = "0"
	defaultLimit  = "100"
	asc           = "ASC"
	desc          = "DESC"
)

// TimeScaleDb is a wrapper on top of a timescale DB connection.
type TimeScaleDb struct {
	Conn *db.DB
	Log  *zap.SugaredLogger
}

// New connects to timescaledb and returns a wrapped connection object.
func New(username, password, host, port, dbName string, log *zap.SugaredLogger) (*TimeScaleDb, error) {
	iport, err := strconv.ParseUint(port, 10, 64)
	if err != nil {
		return nil, err
	}

	ci := &db.ConnectionInfo{Application: "ti-svc", DBName: dbName, User: username, Host: host, Password: password, Port: uint(iport), Engine: "postgres"}
	db, err := db.NewDB(ci, log)
	if err != nil {
		return nil, err
	}

	return &TimeScaleDb{Conn: db, Log: log}, nil
}

// Write writes test cases to DB
func (tdb *TimeScaleDb) Write(ctx context.Context, table, accountId, orgId, projectId, buildId, stageId, stepId, report string, tests ...*types.TestCase) error {
	query := fmt.Sprintf(
		`
		INSERT INTO %s
		(time, account_id, org_id, project_id, build_id, stage_id, step_id, report, name, suite_name,
		class_name, duration_ms, status, message, type, description, stdout, stderr)
		VALUES
		(Now(), $1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11, $12, $13, $14, $15, $16, $17)`, table)

	var merror error

	for _, test := range tests {
		timeMs := test.Duration.Milliseconds()
		status := test.Result.Status

		_, err := tdb.Conn.Exec(query, accountId, orgId, projectId, buildId, stageId, stepId, report, test.Name, test.SuiteName, test.ClassName, timeMs, status,
			test.Result.Message, test.Result.Type, test.Result.Desc, test.SystemOut, test.SystemErr)
		if err != nil {
			// Log the error but continue
			tdb.Log.Errorw("errored while trying to write testcases to DB", zap.Error(err))
			merror = multierror.Append(merror, err)
		}
	}
	return merror
}

// Summary provides test case summary by querying the DB
func (tdb *TimeScaleDb) Summary(ctx context.Context, table, accountId, orgId, projectId, buildId, report string) (types.SummaryResponse, error) {
	query := fmt.Sprintf(`
		SELECT duration_ms, status, name FROM %s WHERE account_id = $1
		AND org_id = $2 AND project_id = $3 AND build_id = $4 AND report = $5;`, table)

	rows, err := tdb.Conn.QueryContext(ctx, query, accountId, orgId, projectId, buildId, report)
	if err != nil {
		tdb.Log.Errorw("could not query database for test summary", zap.Error(err))
		return types.SummaryResponse{}, err
	}
	total := 0
	timeTakenMs := 0
	tests := []types.TestSummary{}
	for rows.Next() {
		var time int
		var status string
		var testName string
		err = rows.Scan(&time, &status, &testName)
		if err != nil {
			// Log error and return
			tdb.Log.Errorw("could not read summary response from DB", zap.Error(err))
			return types.SummaryResponse{}, err
		}
		total++
		timeTakenMs = timeTakenMs + time
		tests = append(tests, types.TestSummary{Name: testName, Status: types.Status(status)})
	}
	if rows.Err() != nil {
		return types.SummaryResponse{}, rows.Err()
	}
	return types.SummaryResponse{Tests: tests, TotalTests: total, TimeMs: timeTakenMs}, nil
}

// GetTestCases returns test cases after querying the DB
func (tdb *TimeScaleDb) GetTestCases(
	ctx context.Context, table, accountID, orgId, projectId, buildId,
	report, suiteName, sortAttribute, status, order, limit, offset string) (types.TestCases, error) {
	statusFilter := "'failed', 'error', 'passed', 'skipped'"
	defaultSortAttribute := "name"
	defaultOrder := asc
	if status == "failed" {
		statusFilter = "'failed', 'error'"
	} else if status != "" {
		return types.TestCases{}, errors.New("status filter only supports 'failed'")
	}
	// default order is to display failed and errored tests first
	failureOrder := `
	CASE
		WHEN status = 'failed' THEN 1
		WHEN status = 'error' THEN 2
		WHEN status = 'skipped' THEN 3
		WHEN status = 'passed' THEN 4
		ELSE 5
	END`
	if offset == "" {
		offset = defaultOffset
	}
	if limit == "" {
		limit = defaultLimit
	}
	if order == "" {
		order = asc
	} else if order != asc && order != desc {
		return types.TestCases{}, errors.New("order must be one of: [ASC, DESC]")
	}
	sortAllowed := []string{"name", "class_name", "status", "duration_ms", ""} // allowed values to sort on
	var ok bool
	for _, s := range sortAllowed {
		if sortAttribute == s {
			ok = true
		}
	}
	if !ok {
		return types.TestCases{}, fmt.Errorf("sorting allowed only for fields: %s", sortAllowed)
	}
	if sortAttribute == "" || sortAttribute == "status" {
		sortAttribute = failureOrder // In case no sort order is specified or we want to sort by status
	}
	query := fmt.Sprintf(
		`
		SELECT name, suite_name, class_name, duration_ms, status, message,
		description, type, stdout, stderr, COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND build_id = $4 AND report = $5 AND suite_name = $6 AND status IN (%s)
		ORDER BY %s %s, %s %s
		LIMIT $7 OFFSET $8;`, table, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, buildId, report, suiteName, limit, offset)
	if err != nil {
		tdb.Log.Errorw("could not query database for test cases", zap.Error(err))
		return types.TestCases{}, err
	}
	tests := []types.TestCase{}
	total := 0
	for rows.Next() {
		var t types.TestCase
		err = rows.Scan(&t.Name, &t.SuiteName, &t.ClassName, &t.Duration, &t.Result.Status, &t.Result.Message,
			&t.Result.Desc, &t.Result.Type, &t.SystemOut, &t.SystemErr, &total)
		if err != nil {
			// Log error and return
			tdb.Log.Errorw("could not read test case response from DB", zap.Error(err))
			return types.TestCases{}, err
		}
		tests = append(tests, t)
	}
	if rows.Err() != nil {
		return types.TestCases{}, rows.Err()
	}
	pageSize, err := strconv.Atoi(limit)
	if err != nil {
		return types.TestCases{}, err
	}
	numPages := total / pageSize
	if total%pageSize != 0 {
		numPages++
	}

	metadata := types.ResponseMetadata{TotalItems: total, PageSize: pageSize, PageItemCount: len(tests), TotalPages: numPages}
	return types.TestCases{Metadata: metadata, Tests: tests}, nil
}

// GetTestSuites returns test suites after querying the DB
func (tdb *TimeScaleDb) GetTestSuites(
	ctx context.Context, table, accountID, orgId, projectId, buildId,
	report, sortAttribute, status, order, limit, offset string) (types.TestSuites, error) {
	defaultSortAttribute := "suite_name"
	defaultOrder := asc
	statusFilter := "'failed', 'error', 'passed', 'skipped'"
	if status == "failed" {
		statusFilter = "'failed', 'error'"
	} else if status != "" {
		return types.TestSuites{}, errors.New("status filter only supports 'failed'")
	}
	if offset == "" {
		offset = defaultOffset
	}
	if limit == "" {
		limit = defaultLimit
	}
	sortAllowed := []string{"suite_name", "duration_ms", "total_tests", "skipped_tests", "passed_tests", "failed_tests", "fail_pct", ""} // allowed values to sort on
	var ok bool
	for _, s := range sortAllowed {
		if sortAttribute == s {
			ok = true
		}
	}
	if !ok {
		return types.TestSuites{}, fmt.Errorf("sorting allowed only for fields: %s", sortAllowed)
	}
	// If sort attribute is not set, set it to failure rate
	if sortAttribute == "" {
		sortAttribute = "fail_pct"
		order = desc
	}
	// If order is not set, use ascending order
	if order == "" {
		order = asc
	} else if order != asc && order != desc {
		return types.TestSuites{}, errors.New("order must be one of: [ASC, DESC]")
	}
	query := fmt.Sprintf(
		`
		SELECT suite_name, SUM(duration_ms) AS duration_ms, COUNT(*) AS total_tests,
		SUM(CASE WHEN status = 'skipped' THEN 1 ELSE 0 END) AS skipped_tests,
		SUM(CASE WHEN status = 'passed' THEN 1 ELSE 0 END) AS passed_tests,
		SUM(CASE WHEN status = 'failed' OR status = 'error' THEN 1 ELSE 0 END) AS failed_tests,
		SUM(CASE WHEN status = 'failed' OR status = 'error' THEN 1 ELSE 0 END) * 100 / COUNT(*) AS fail_pct,
		COUNT(*) OVER() AS full_count
		FROM %s
		WHERE account_id = $1 AND org_id = $2 AND project_id = $3 AND build_id = $4 AND report = $5 AND status IN (%s)
		GROUP BY suite_name
		ORDER BY %s %s, %s %s
		LIMIT $6 OFFSET $7;`, table, statusFilter, sortAttribute, order, defaultSortAttribute, defaultOrder)
	rows, err := tdb.Conn.QueryContext(ctx, query, accountID, orgId, projectId, buildId, report, limit, offset)
	if err != nil {
		tdb.Log.Errorw("could not query database for test suites", "error_msg", err)
		return types.TestSuites{}, err
	}
	testSuites := []types.TestSuite{}
	total := 0
	for rows.Next() {
		var t types.TestSuite
		err = rows.Scan(&t.Name, &t.TimeMs, &t.TotalTests, &t.SkippedTests, &t.PassedTests, &t.FailedTests, &t.FailPct, &total)
		if err != nil {
			// Log the error and return
			tdb.Log.Errorw("could not read suite response from DB", zap.Error(err))
			return types.TestSuites{}, err
		}
		testSuites = append(testSuites, t)
	}
	if rows.Err() != nil {
		return types.TestSuites{}, rows.Err()
	}
	pageSize, err := strconv.Atoi(limit)
	if err != nil {
		return types.TestSuites{}, err
	}
	numPages := total / pageSize
	if total%pageSize != 0 {
		numPages++
	}

	metadata := types.ResponseMetadata{TotalItems: total, PageSize: pageSize, PageItemCount: len(testSuites), TotalPages: numPages}
	return types.TestSuites{Metadata: metadata, Suites: testSuites}, nil
}
