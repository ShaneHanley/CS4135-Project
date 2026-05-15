package com.pharmacy.pharmacy.exception;

public class IllegalStateTransitionException extends RuntimeException {
  public IllegalStateTransitionException(String from, String to) {
    super("Invalid status transition: " + from + " -> " + to);
  }
}
