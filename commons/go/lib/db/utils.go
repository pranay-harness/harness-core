// Copyright 2021 Harness Inc.
// 
// Licensed under the Apache License, Version 2.0
// http://www.apache.org/licenses/LICENSE-2.0

package db

import (
	"hash/fnv"
	"strings"
	"time"

	"go.uber.org/zap"
)

func logQuery(log *zap.SugaredLogger, start time.Time, query string, args []interface{}, err error) {
	logw := log.Infow
	if err != nil {
		logw = log.Errorw
	}

	// Log only the first 50 args and the first 300 characters of the SQL query to avoid spamming the logs
	logw("sql query execute", "sql.query", truncateString(collapseSpaces(query), 300), "sql.hash", hash(query),
		"logQuerysql.parameters", truncateList(args, 50), "query_time_ms", ms(time.Since(start)), zap.Error(err))
}

func truncateList(inp []interface{}, to int) []interface{} {
	if len(inp) > to {
		return inp[:to]
	}
	return inp
}

func truncateString(inp string, to int) string {
	if len(inp) > to {
		return inp[:to]
	}
	return inp
}

// collapseSpaces standardizes string by removing multiple spaces between words
func collapseSpaces(s string) string {
	return strings.Join(strings.Fields(s), " ")
}

func hash(s string) uint32 {
	h := fnv.New32a()
	h.Write([]byte(s))
	return h.Sum32()
}

func ms(d time.Duration) float64 {
	return float64(d) / float64(time.Millisecond)
}
