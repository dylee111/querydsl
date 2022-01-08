package study.querydsl.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

@Profile("local")
@Component
@RequiredArgsConstructor
public class InitMember {

    private final InitMemberService initMemberService;

    // PostConstruct와 Transactional은 분리해서 정의
    @PostConstruct
    public void init() {
        initMemberService.init();
    }

    @Component
    static class InitMemberService {

        @PersistenceContext
        EntityManager em;

        @Transactional
        public void init() {
            Team team1 = new Team("TeamA");
            Team team2 = new Team("TeamB");
            em.persist(team1);
            em.persist(team2);

            for (int i = 0; i < 100; i++) {
                Team selectedTeam = i % 2 == 0 ? team1 : team2;
                em.persist(new Member("Member" + i, i, selectedTeam));
            }

        }
    }
}
