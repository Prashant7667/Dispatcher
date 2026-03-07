package org.dispatchsystem.common.exceptions;
import java.time.LocalDateTime;

public class ApiError {
    private int status;
    private String error;
    private String message;
    private LocalDateTime timestamp;
    public ApiError(int status, String error, String message){
        this.status=status;
        this.error=error;
        this.message=message;
        this.timestamp=LocalDateTime.now();
    }
}
