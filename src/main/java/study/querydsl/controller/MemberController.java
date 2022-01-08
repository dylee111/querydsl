package study.querydsl.controller;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.repository.MemberJpaRepository;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class MemberController {

    private final MemberJpaRepository memberJpaRepository;

    @GetMapping("/v1/members")
    public FindMemberResult searchV1(MemberSearchCond condition) {
        List<MemberTeamDto> result = memberJpaRepository.searchWhereParam(condition);
        return new FindMemberResult(result.size(),result);
    }

    @Data
    static class FindMemberResult<T> {
        private int count;
        private T data;

        public FindMemberResult(T data) {
            this.data = data;
        }

        public FindMemberResult(int count, T data) {
            this.count = count;
            this.data = data;
        }
    }
}
