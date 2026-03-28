package com.cap.leave.exception;

public class LeaveNotFoundException extends ResourceNotFoundException {
    public LeaveNotFoundException(String message) {
        super(message);
    }
}
