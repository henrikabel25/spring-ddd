package com.springddd.application.service.user;

import com.springddd.domain.auth.ReactiveSecurityUtils;
import com.springddd.domain.user.DeleteSysUserByIdDomainService;
import com.springddd.domain.user.SysUserDomainRepository;
import com.springddd.domain.user.UserId;
import com.springddd.domain.user.exception.UserNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DeleteSysUserByIdDomainServiceImpl implements DeleteSysUserByIdDomainService {

    private final SysUserDomainRepository sysUserDomainRepository;

    @Override
    public Mono<Void> deleteByIds(List<Long> ids) {
        return Flux.fromIterable(ids)
                .flatMap(id -> sysUserDomainRepository.load(new UserId(id))
                        .switchIfEmpty(Mono.error(new UserNotFoundException(id)))
                        .flatMap(domain -> {
                            domain.delete();
                            return sysUserDomainRepository.save(domain);
                        }), ReactiveSecurityUtils.concurrency())
                .then();
    }
}
