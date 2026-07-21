package com.springddd.domain.user.exception;

import com.springddd.domain.DomainException;
import com.springddd.domain.util.ErrorCode;
import lombok.Getter;

@Getter
public class UserNotFoundException extends DomainException {

    public UserNotFoundException(Long userId) {
        super(ErrorCode.USER_NOT_FOUND, userId);
    }
}
