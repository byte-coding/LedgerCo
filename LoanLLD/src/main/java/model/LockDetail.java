package model;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Date;

@Data
@AllArgsConstructor
public class LockDetail<T> {
    private T data;
    private Date lockTime;
    private int lockDurationInSeconds;
}
