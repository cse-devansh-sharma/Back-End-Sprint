package com.cap.timesheet.exception;

public class HolidayClashException extends BusinessRuleException {
    public HolidayClashException(String message) {
        super(message);
    }
}
