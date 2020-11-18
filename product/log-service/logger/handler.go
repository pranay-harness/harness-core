package logger

import (
	"net/http"
	"time"

	"github.com/gofrs/uuid"
	"github.com/sirupsen/logrus"
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
		log := FromContext(ctx).WithField("request-id", id)
		accountID := r.FormValue("accountID")
		log = log.WithFields(logrus.Fields{
			"accountID": accountID,
			"method":    r.Method,
			"request":   r.RequestURI,
			"remote":    r.RemoteAddr,
		})
		ctx = WithContext(ctx, log)
		start := time.Now()
		next.ServeHTTP(w, r.WithContext(ctx))
		end := time.Now()
		log = log.WithFields(logrus.Fields{
			"latency": end.Sub(start),
			"time":    end.Format(time.RFC3339),
		})
		log.Infof("Completed request")
	})
}
