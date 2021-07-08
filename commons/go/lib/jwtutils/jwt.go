package jwtutils

import (
	"fmt"
	"time"

	"github.com/golang-jwt/jwt"
)

// Verify method verifies JWT token
func Verify(tokenString, secretKey string) error {
	token, err := jwt.Parse(tokenString, func(token *jwt.Token) (interface{}, error) {
		return []byte(secretKey), nil
	})

	if err == nil && token != nil && token.Valid {
		return nil
	}

	return fmt.Errorf("Invalid token: %s", err)
}

// GenerateToken generates a standard JWT token
func GenerateToken(secretKey string, tokenDuration int64) (string, error) {
	// Create the Claims
	token := jwt.NewWithClaims(jwt.SigningMethodHS256, jwt.MapClaims{
		"exp": time.Now().Add(time.Duration(tokenDuration) * time.Second).Unix(),
	})

	return token.SignedString([]byte(secretKey))
}
