package com.springddd.domain.dict;

import reactor.core.publisher.Mono;

import java.util.List;

public interface DeleteSysDictByIdDomainService {

    Mono<Void> deleteByIds(List<Long> ids);
}
