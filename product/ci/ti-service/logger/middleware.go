package logger

import (
	"net/http"
	"strings"
	"time"

	"github.com/gofrs/uuid"
	"go.uber.org/zap"
)

// Middleware provides logging middleware.
func Middleware(next http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		id := r.Header.Get("X-Request-ID")
		if id == "" {
			uuid, _ := uuid.NewV4()
			id = uuid.String()
		}
		ctx := r.Context()
		log := FromContext(ctx).With(zap.String("request-id", id))
		accountID := r.FormValue("accountID")
		log = log.With(
			"accountID", accountID,
			"method", r.Method,
			"request", r.RequestURI,
			"remote", r.RemoteAddr,
		)

		ctx = WithContext(ctx, log)
		start := time.Now()
		next.ServeHTTP(w, r.WithContext(ctx))
		end := time.Now()
		log = log.With(
			"latency", end.Sub(start),
			"time", end.Format(time.RFC3339),
		)
		// Don't spam ti service logs with health checks
		if !strings.Contains(r.RequestURI, "/healthz") {
			log.Infof("completed request")
		}
	})
}
