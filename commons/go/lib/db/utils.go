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

	// Log only the first 100 args to avoid spamming the logs
	logw("sql query execute", "sql.query", collapseSpaces(query), "sql.hash", hash(query),
		"logQuerysql.parameters", truncate(args, 100), "query_time_ms", ms(time.Since(start)), zap.Error(err))
}

func truncate(inp []interface{}, to int) []interface{} {
	if len(inp) > to {
		return inp[:to]
	}
	return inp
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
