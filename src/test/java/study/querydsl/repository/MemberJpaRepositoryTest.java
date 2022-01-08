package study.querydsl.repository;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberSearchCond;
import study.querydsl.dto.MemberTeamDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class MemberJpaRepositoryTest {

    @Autowired
    EntityManager em;
    @Autowired
    MemberJpaRepository memberJpaRepository;

    /*
    * 순수 JPA 코드 Test
    * */
    @Test
    public void BasicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findById(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAll();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsername("member1");
        assertThat(result2).containsExactly(member);

    }

    /*
    * QueryDsl 코드 Test
    * */
    @Test
    public void querydslBasicTest() {
        Member member = new Member("member1", 10);
        memberJpaRepository.save(member);

        Member findMember = memberJpaRepository.findByIdQuerydsl(member.getId()).get();
        assertThat(findMember).isEqualTo(member);

        List<Member> result1 = memberJpaRepository.findAllQuerydsl();
        assertThat(result1).containsExactly(member);

        List<Member> result2 = memberJpaRepository.findByUsernameQuerydsl("member1");
        assertThat(result2).containsExactly(member);
    }

    @Test
    public void 동적쿼리_Builder() {
        Team team1 = new Team("TeamA");
        Team team2 = new Team("TeamB");
        em.persist(team1);
        em.persist(team2);

        memberJpaRepository.save(new Member("Member1",10,team1));
        memberJpaRepository.save(new Member("Member2",20,team1));
        memberJpaRepository.save(new Member("Member3",30,team2));
        memberJpaRepository.save(new Member("Member4",40,team2));

        MemberSearchCond cond = new MemberSearchCond();
        cond.setTeamName("TeamB");
        cond.setAgeGoe(32);
        cond.setAgeLoe(40);

//        List<MemberTeamDto> result = memberJpaRepository.searchByBuilder(cond);
        List<MemberTeamDto> result = memberJpaRepository.searchWhereParam(cond);

        assertThat(result).extracting("username").containsExactly("Member4");
    }

}