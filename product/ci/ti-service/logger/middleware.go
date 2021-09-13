package logger

import (
	"net/http"

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
		accountID := r.FormValue("accountId")
		log = log.With(
			"accountId", accountID,
			"method", r.Method,
			"request", r.RequestURI,
			"remote", r.RemoteAddr,
		)

		ctx = WithContext(ctx, log)
		next.ServeHTTP(w, r.WithContext(ctx))
	})
}
