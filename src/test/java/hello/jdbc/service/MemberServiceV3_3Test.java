package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
@Slf4j
@SpringBootTest
class MemberServiceV3_3Test {
    public static final String MEMBER_A = "memberA";
    public static final String MEMBER_B = "memberB";
    public static final String MEMBER_EX = "ex";

//    @SpringBootTest : 스프링 AOP를 적용하려면 스프링 컨테이너가 필요하다. 이 애노테이션이 있으면 테스트시 스프링 부트를 통해
//    스프링 컨테이너를 생성한다. 그리고 테스트에서 @Autowired등을 통해 스프링 컨테이너가 관리하는 빈들을 사용할 수 있다.

//    @SpringBootTest가 없다면 스프링 컨테이너에서 테스트가 이뤄지지 않고 단순히 순순한 자바 객체들 간의 의존관계로 이루어진 context에서
//    테스트가 실행이 된다. 이렇게 한다면 스프링이 스프링컨테이너에 기본으로 등록하는 빈들을 사용할 수 없다. 스프링이 제공하는 트랜잭션 AOP인
//    프록시를 사용하기 위해서는 우리가 만든 자바 객체(빈)들간의 의존관계 뿐만 아니라 스프링이 기본으로 스프링 컨테이너에 등록하는 빈들도 필요하다.
//    따라서 스프링에 제공하는 트랜잭션 AOP를 적용한 프록시를 사용하기 위해서는 스프링이 관리하는 스프링 컨테이너 환경에서,
//    스프링 컨테이너 안에서 테스트가 진행 되어야 한다.

    @Autowired
    private MemberRepositoryV3 memberRepository;
    @Autowired
    private MemberServiceV3_3 memberService;
//    우리는 지금 MemberServiceV3_3Test를 하나의 빈으로 만드는 것이 아니라 MemberServiceV3_3Test클래스에서 테스트를 실행하는데 이때에
//    테스트를 할때 그 테스트를 하기 위해서 사용되는 환경(리소스)이 우리가 정의한 순수 자바 객체들 간의 의존관계가 이뤄진 context를
//    사용하는 것이 아니라 스프링 컨테이너에 등록된 객체(빈)들의 의존관계가 이뤄진 context를 사용해서 테스트를 진행하고 싶은것이다.
//    따라서 우선 @SpringBootTest를 써줬다. 그리고 나서 MemberServiceV3_3Test객체를 생성하고 빈으로 등록함 으로서의 생성자로
//    의존관계를 주입받는 것이 아니라 MemberServiceV3_3Test내에 있는 스프링 컨테이너 context 자원을 사용할 것이기 때문에
//    memberRepository와 memberService는 필드 주입을 통해서 빈으로 만드어 줬다. 그리고 그 2개의 빈은 @SpringBootTest로 인해서
//    스프링이 관리하는 스프링 컨테이너에 스프링이 기본으로 제공하는 빈들과 함께 등록되어 있다.
//
//    자.. 이제 우리가 할 일은 MemberServiceV3_3Test가 스프링 컨테이너 환경에서 빈들(리소스)를 사용하는데 추가적으로 더 필요한
//    스프링 컨테이너 안의 빈을 등록을 해 주는 일이다. 따라서 그러한 작업을 하기 위해서 MemberServiceV3_Test클래스가 사용하는
//    스프링 컨테이너 안에 빈들을 등록시켜 줘야 겠다. 따라서 그러한 일을 하기 위해서 @TestConfiguration을 사용했다.
//    @TestConfiguration안에서 등록한 빈들이 테스트 환경, MemberServiceV3_3Test가 사용하는 스프링 컨테이너 안에 빈으로 등록된다.
//    dataSource, transactionManager, memberRepositoryV3, memberServiceV3_3을 빈으로 등록했다.

    @TestConfiguration
    static class TestConfig{
//        @TestConfiguration : 테스트 안에서 내부 설정 클래스를 만들어서 사용하면서 이 네노테이션을 붙이면, 스프링 부트가 자동으로
//        만들어주는 빈들에 추가로 필요한 스프링 빈들을 등록하고 테스트를 수행할 수 있다.
        @Bean
        DataSource dataSource(){
            return new DriverManagerDataSource(URL, USERNAME, PASSWORD);
        }
//        DataSource : 스프링에서 기본으로 사용할 데이터소스를 빈으로 등록한다. 추가로 트랜잭션 매니저에서도 사용한다.
        @Bean
        PlatformTransactionManager transactionManager(){
            return new DataSourceTransactionManager(dataSource());
        }
//        DataSourceTransactionManager : 트랜잭션 매니저를 스프링 빈으로 등록한다. 트랜잭션 AOP가 적용된 프록시를 사용하기 때문에
//        없어도 될 것 같지만 기본적으로 트랜잭션 매니저도 스프링 컨테이너에 빈으로 등록이 되어 있어야 한다. 스프링이 제공하는 트랜잭션 AOP는
//        스프링 빈에 등록된 트랜잭션 매니저를 찾아서 사용하기 때문에 트랜잭션 매니저를 스프링 빈으로 등록해두어야 한다.

//        MemberServiceV3_3에서 트랜잭션 AOP가 적용된 프록시를 적용받아서 테스트 하려면 프록시에서는 dataSource와 transactionManager을
//        가져다가 쓰기 때문에 dataSource와 transactionManager을 스프링 컨테이너에 빈으로 등록시켜야 한다.
        @Bean
        MemberRepositoryV3 memberRepositoryV3(){
            return new MemberRepositoryV3(dataSource());
        }
        @Bean
        MemberServiceV3_3 memberServiceV3_3(){
            return new MemberServiceV3_3(memberRepositoryV3());
        }
    }

//    @BeforeEach
//    void before(){
//        DriverManagerDataSource dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
//        PlatformTransactionManager transactionManager = new DataSourceTransactionManager(dataSource);
//        memberRepository = new MemberRepositoryV3(dataSource);
//        memberService = new MemberServiceV3_3(transactionManager, memberRepository);
//    }

    @AfterEach
    void after() throws SQLException {
        memberRepository.delete(MEMBER_A);
        memberRepository.delete(MEMBER_B);
        memberRepository.delete(MEMBER_EX);
    }

    @Test
    @DisplayName("정상 이체")
    void accountTransfer() throws SQLException{
        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberB = new Member(MEMBER_B, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberB);
        // when
        memberService.accountTransfer(memberA.getMemberId(), memberB.getMemberId(), 2000);
        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberB = memberRepository.findById(memberB.getMemberId());
        assertThat(findMemberA.getMoney()).isEqualTo(8000);
        assertThat(findMemberB.getMoney()).isEqualTo(12000);
    }

    @Test
    @DisplayName("이체중 예외 발생")
    void accountTransferEx() throws SQLException{
        // given
        Member memberA = new Member(MEMBER_A, 10000);
        Member memberEx = new Member(MEMBER_EX, 10000);
        memberRepository.save(memberA);
        memberRepository.save(memberEx);
        // when
        assertThatThrownBy(()->memberService.accountTransfer(memberA.getMemberId(), memberEx.getMemberId(), 2000))
                .isInstanceOf(IllegalStateException.class);
        // then
        Member findMemberA = memberRepository.findById(memberA.getMemberId());
        Member findMemberEx = memberRepository.findById(memberEx.getMemberId());
        // memberA의 돈이 롤백 되어야 합
        assertThat(findMemberA.getMoney()).isEqualTo(10000);
        assertThat(findMemberEx.getMoney()).isEqualTo(10000);
    }

    @Test
    @DisplayName("Aop Check")
    void aopCheck(){
        log.info("memberService class={}", memberService.getClass());
        // memberService안에 들어있는 MemberServiceV3_3으로 만들어진 빈이 과연 어떤 클래스로 만들어 졌는지 보여준다.
        log.info("memberRepository class={}", memberRepository.getClass());
        // memberRepository안에 들어있는 MemberRepositoryV3으로 만들어진 빈이 과연 어떤 클래스로 만들어 졌는지 보여준다.

//        먼저 AOP 프록시가 적용되었는지 확인해보자.AopCheck()의 실행 결과를 보면 memberService에 EnhancerBySpringCGLIB..라는 부분을
//        통해서 프록시(CGLIB)가 적용된 것을 확인할 수 있다. MemberServiceV3_3$$EnhancerBySpringCGLIB$$fd086a5d 이것은 실제 MemberServiceV3_3이 아니라
//        트랜잭션 AOP가 적용된 프록시 코드이다. 그리고 트랜잭션 프록시 코드는 내부에 트랜잭션을 처리하는 로직이 있다. 그리고 트랜잭션 프록시 코드는 실제 서비스에
//        ,여기서는 MemberServiceV3_3, 타겟을 호출하는 그런 코드들도 내부에서 다 포함하고 있다고 보면 된다. 우리는 @Autowired로 실제 서비스
//        (MemberServiceV3_3으로 만들어진 빈)를 받은 것이 아니라 스프링이 트랜잭션 AOP가 적용된 프록시를 스프링 빈으로 등록하고 실제 MemberServiceV3_3으로 만들어진 빈
//        앞에 두었기 때문에 우리가 @Autowired로 트랜잭션 프로시를 받은 것이다. memberRepository에는 AOP를 적용하지 않았기 때문에 프록시가 적용되지 않았다.

//        AOP 프록시가 적용되었는지 콘솔창에서 눈으로 확인할 수만은 없으니까, 더 쉬운방법으로 초록불을 띄게끔 하기위해서, 다시말해 AOP 프록시가 잘 적용되었는지
//        테스트 하기 위해서 AopUtils라는 것을 스프링이 제공을 한다. AOP 프록시 냐고 물어보는 것이다.
        Assertions.assertThat(AopUtils.isAopProxy(memberService)).isTrue();
        Assertions.assertThat(AopUtils.isAopProxy(memberRepository)).isFalse();

//        => 나머지 테스트 코드들을 실행해 보면 트랜잭션이 정상 수행되고, 실패시 정상 롤백되는 것을 확인할 수 있다.
     }

}