package utils

import "time"

//TimeSince returns the number of milliseconds that have passed since the given time
func TimeSince(t time.Time) float64 {
	return Ms(time.Since(t))
}

//Ms returns the duration in millisecond
func Ms(d time.Duration) float64 {
	return float64(d) / float64(time.Millisecond)
}

//NoOp is a basic NoOp function
func NoOp() error {
	return nil
}
