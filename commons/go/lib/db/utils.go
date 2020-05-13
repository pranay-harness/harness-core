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
		logw = log.Warnw
	}

	logw("sql query execute", "sql.query", collapseSpaces(query), "sql.hash", hash(query),
		"sql.parameters", args, "query_time_ms", ms(time.Since(start)), zap.Error(err))
}

// collapseSpaces standardizes string by removing multiple spaces between words
func collapseSpaces(s string) interface{} {
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
