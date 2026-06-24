package com.splitit.domain.expense.exception;

/** Thrown when expense split parameters are invalid (bad percentages, bad amounts, etc.). */
public class InvalidSplitException extends RuntimeException {

    public InvalidSplitException(String message) {
        super(message);
    }
}
