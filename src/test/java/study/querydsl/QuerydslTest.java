package study.querydsl;

import com.querydsl.core.BooleanBuilder;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.core.types.dsl.NumberExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.QMemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.team;

@SpringBootTest
@Transactional
public class QuerydslTest {

    @Autowired
    private EntityManager em;

    JPAQueryFactory queryFactory;

    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);

        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        em.persist(new Member("member1", 10, teamA));
        em.persist(new Member("member2", 20, teamA));
        em.persist(new Member("member3", 30, teamB));
        em.persist(new Member("member4", 40, teamB));
    }

    @Test
    public void startJPQL() {
        //member1 조회
        String qlString =
                "SELECT m FROM Member m " +
                        "WHERE m.username = :username";
        Member findMember = em.createQuery(qlString, Member.class)
                .setParameter("username", "member1")
                .getSingleResult();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member findMember = queryFactory
                .selectFrom(QMember.member)
                .where(QMember.member.username.eq("member1")
                        .and(QMember.member.age.eq(10)))
                .fetchOne();
        assertThat(findMember.getUsername()).isEqualTo("member1");
    }

    @Test
    public void searchAndParam() {
        List<Member> findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"),
                        member.age.eq(10))
                .fetch();
        assertThat(findMember.size()).isEqualTo(1);
    }

    @Test
    public void resultFetch() {
        Member findMember = queryFactory.selectFrom(QMember.member).fetchOne(); // 단 건 조회

        List<Member> findMembers = queryFactory.selectFrom(member).fetch(); // 리스트 전체 조회

        Member fetchFirst = queryFactory.selectFrom(QMember.member).fetchFirst(); // limit(1).fetchOne();

        // fetchResults() / fetchCount()는 deprecated됨
    }

    @Test
    public void sort() {

        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));
        em.persist(new Member(null, 100));

        List<Member> sortMember = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(),
                        member.username.asc().nullsLast())
                .fetch();

        assertThat(sortMember.get(0).getUsername()).isEqualTo("member5");
        assertThat(sortMember.get(1).getUsername()).isEqualTo("member6");
        assertThat(sortMember.get(2).getUsername()).isNull();
    }

    @Test
    public void paging() {
        List<Member> paging = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(3)
                .fetch();
        long totalCount = paging.size();
        for (Member member1 : paging) {
            System.out.println("member1.getUsername() = " + member1.getUsername());
        }
        System.out.println("totalCount = " + totalCount);
        assertThat(paging.size()).isEqualTo(3);
    }

    @Test
    public void 집합함수() {
        // querydsl의 Tuple
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.avg(),
                        member.age.sum(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);

        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);

        System.out.println("COUNT >>> " + tuple.get(member.count()));
        System.out.println("AVERAGE >>> " + tuple.get(member.age.avg()));
        System.out.println("SUM >>> " + tuple.get(member.age.sum()));
        System.out.println("MAX >>> " + tuple.get(member.age.max()));
        System.out.println("MIN >>> " + tuple.get(member.age.min()));
    }

    /*
    * 팀 이름과 각 팀의 평균 연령
    * */
    @Test
    public void groupBy() {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        System.out.println("teamA.get(team.name) = " + teamA.get(team.name));
        System.out.println("-> 평균 연령 = " + teamA.get(member.age.avg()));
        System.out.println("teamB.get(team.name) = " + teamB.get(team.name));
        System.out.println("-> 평균 연령 = " + teamB.get(member.age.avg()));

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);
        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);
    }

    /*
    * TeamA에 속한 모든 회원
    * */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

        for (Member member1 : result) {
            System.out.println("member = " + member1);
        }
    }

    /*
     * 팀 이름과 동일한 회원 조회
     * */
    @Test
    public void thetaJoin() {

        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");

        for (Member member1 : result) {
            System.out.println("member = " + member1);
        }
    }

    /**
     * 예) 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     * JPQL: SELECT m, t FROM Member m LEFT JOIN m.team t on t.name = 'teamA'
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.TEAM_ID=t.id and
     * t.name='teamA'
     */
    @Test
    public void on사용_필터링() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team)
//                .on(team.name.eq("teamA"))
                .where(team.name.eq("teamA"))
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * 2. 연관관계 없는 엔티티 외부 조인
     * 예) 회원의 이름과 팀의 이름이 같은 대상 외부 조인
     * JPQL: SELECT m, t FROM Member m LEFT JOIN Team t on m.username = t.name
     * SQL: SELECT m.*, t.* FROM Member m LEFT JOIN Team t ON m.username = t.name
     */
    @Test
    public void 연관관계_없는_외부조인() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @PersistenceUnit
    EntityManagerFactory entityManagerFactory;

    @Test
    public void querydslFetchJoinUse() {
        em.flush();
        em.clear(); // 테스트 전 영속성 컨테스트 초기화

        Member result = queryFactory
                .select(member)
                .from(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();

        // member를 호출하는 시점에 team이 호출되어 있는지 여부 확인
        boolean loaded = entityManagerFactory
                .getPersistenceUnitUtil().isLoaded(result.getTeam());

        // 호출되었으면 true
        assertThat(loaded).as("페치 조인 적용").isTrue();
        System.out.println("loaded = " + loaded);
    }

    /*
    * 제일 나이 많은 회원
    * */
    @Test
    public void whereSubQueryEq() {
        // 예제에서는 동일한 Member를 사용하기 때문에 서브 쿼리용 QMember 추가
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions // static import 가능
                                .select(memberSub.age.max())
                                .from(memberSub)))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
    * 나이가 평균 이상인 회원
    * */
    @Test
    public void whereSubQueryGoe() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(30, 40);
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
    * 서브쿼리 여러 건 처리, in 사용
    * 10살보다 나이가 많은 회원
    * */
    @Test
    public void whereSubQueryIn() {

        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10))))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
    * 회원 이름과 회원 평균 나이
    * */
    @Test
    public void selectSubQuery() {

        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                )
                .from(member)
                .fetch();
        for (Tuple tuple : result) {
            System.out.println("username = " + tuple.get(member.username));
            System.out.println(" -> age = " + tuple.get(JPAExpressions.select(memberSub.age.avg()).from(memberSub)));
        }
    }

    /*
    * 간단한 case문
    * */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(
                        member.age
                                .when(10).then("열살")
                                .when(20).then("스무살")
                                .otherwise("기타"))
                .from(member)
                .fetch();
        for (String memberAge : result) {
            System.out.println("memberAge = " + memberAge);
        }
    }

    /*
     * 복잡한 case문
     * */
    @Test
    public void complexCase() {
        List<String> result = queryFactory.select(new CaseBuilder()
                        .when(member.age.between(10, 19)).then("10대")
                        .when(member.age.between(20, 29)).then("20대")
                        .when(member.age.between(30, 39)).then("30대")
                        .when(member.age.between(40, 49)).then("40대")
                        .otherwise("기타"))
                .from(member)
                .fetch();
        for (String member : result) {
            System.out.println("member = " + member);
        }
    }

    /*
     * order by case문
     * 1. 0 ~ 30살이 아닌 회원을 가장 먼저 출력
     * 2. 0 ~ 20살 회원 출력
     * 3. 21 ~ 30살 회원 출력
     * */
    @Test
    public void orderByCase() {
        NumberExpression<Integer> rankPath = new CaseBuilder()
                .when(member.age.between(0, 20)).then(2)
                .when(member.age.between(21, 30)).then(1)
                .otherwise(3);

        List<Tuple> result = queryFactory
                .select(member.username, member.age, rankPath)
                .from(member)
                .orderBy(rankPath.desc())
                .fetch();
        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            Integer rank = tuple.get(rankPath);

            System.out.println("USERNAME = " + username + " / AGE = " + age + " / RANK = " + rank);
        }
    }

    /*
    * 상수
    * Expressions.constant()
    * */
    @Test
    public void 상수() {

        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("상수"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /*
     * 문자 더하기
     * concat()
     * */
    @Test
    public void 문자더하기() {

        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member2"))
                .fetch();

        for (String member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
     * DTO 조회 -> Setter(프로퍼티)
     * */
    @Test
    public void findByDtoSetter() {
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * DTO 조회 -> 필드
     * */
    @Test
    public void findByDtoField() {

        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * DTO 조회 -> 필드 (별칭이 다른 경우)
     * */
    @Test
    public void findByDtoAlias() {

        QMember mSub = new QMember("mSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        ExpressionUtils.as(
                                JPAExpressions // 서브쿼리 결과인 최대 나이를 age 필드에 매핑
                                        .select(mSub.age.max())
                                        .from(mSub) ,"age")
                        ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    /*
    * DTO 조회 -> 생성자
    * */
    @Test
    public void findByDtoConstructor() {

        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class,
                        member.username,
                        member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
     * DTO 조회 -> @QueryProjection
     * */
    @Test
    public void findByDtoQueryProjection() {

        List<MemberDto> result = queryFactory
                .select(new QMemberDto(member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /*
    * 동적 쿼리 -> BooleanBuilder 활용
    * */
    @Test
    public void 동적쿼리_BooleanBuilder() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember1(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    private List<Member> searchMember1(String usernameCond, Integer ageCond) {
        BooleanBuilder builder = new BooleanBuilder();

        if (usernameCond != null) {
            builder.and(member.username.eq(usernameCond));
        }
        if (ageCond != null) {
            builder.and(member.age.eq(ageCond));
        }

        return queryFactory
                .selectFrom(member)
                .where(builder)
                .fetch();
    }

    /*
     * 동적 쿼리 -> WHERE 다중 파라미터 사용
     * */
    @Test
    public void 동적쿼리_Where_다중_파라미터() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
        assertThat(result.size()).isEqualTo(1);
    }

    @Test
    public void 동적쿼리_Where_Dto() {
        String usernameParam = "member1";
        Integer ageParam = 10;

        List<Member> result = searchMember2(usernameParam, ageParam);
    }

    private List<Member> searchMember2(String usernameCond, Integer ageCond) {
        return queryFactory
                .selectFrom(member)
                .where(nameEq(usernameCond), ageEq(ageCond))
                .fetch();
    }

    private BooleanExpression ageEq(Integer ageCond) {
        return ageCond != null ? member.age.eq(ageCond) : null;
    }

    private BooleanExpression nameEq(String usernameCond) {
        return usernameCond != null ? member.username.eq(usernameCond) : null;
    }

    private BooleanExpression allEq(String usernameCond, Integer ageCond) {
        return nameEq(usernameCond).and(ageEq(ageCond));
    }

    /*
    * 벌크 연산 -> UPDATE
    * */
    @Test
//    @Commit
    public void bulkUpdate() {
        long count = queryFactory
                .update(member)
                .set(member.username, "비회원")
                .where(member.age.lt(28))
                .execute();

        em.flush();
        em.clear();

        List<Member> list = queryFactory
                .selectFrom(member)
                .fetch();
        for (Member member1 : list) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
     * 벌크 연산 -> DELETE
     * */
    @Test
//    @Commit
    public void bulkDelete() {
        queryFactory
                .delete(member)
                .where(member.age.gt(21))
                .execute();

        em.flush();
        em.clear();

        List<Member> result = queryFactory
                .selectFrom(member)
                .fetch();

        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
     * 사칙연산
     * */
    @Test
    public void bulkAdd() {
        queryFactory
                .update(member)
                .set(member.age, member.age.add(1)) // 빼기는 .add(-1) 곱하기는 .multiply(2) 나누기는 .divide(2)
                .execute();
        em.flush();
        em.clear();

        List<Member> result = queryFactory.selectFrom(member).fetch();
        for (Member member1 : result) {
            System.out.println("member1 = " + member1);
        }
    }

    /*
     * SQL Function
     * */
    @Test
    public void sqlFunctionReplace() {
        List<String> result = queryFactory
                .select(Expressions
                        .stringTemplate("function('replace', {0}, {1}, {2})", member.username, "member", "M"))
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void sqlFunctionLower() {
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
//                .where(member.username.eq(Expressions
//                        .stringTemplate("function('lower', {0})", member.username)))
                .where(member.username.eq(member.username.lower()))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }
}
