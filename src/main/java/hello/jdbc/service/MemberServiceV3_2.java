package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.function.Consumer;

/*
* 트랜잭션 - 트랜잭션 템플릿
* */
@Slf4j
//@RequiredArgsConstructor
public class MemberServiceV3_2 {

//    private final PlatformTransactionManager transactionManager;
//    우선 먼저 PlatformTransactionManager 부분을 주석처리 했다. 이전에는 트랜잭션 매니저를 주입받아서 받은 트랜잭션 매니저를 이용해서
//    트랜잭션을 시작하고 트랜잭션을 종료 했는데 그렇게 하면 트랜잭션을 시작하는 모든 비즈니스 로직에 트랜잭션 시작하는 코드와 try, catch문과
//    트랜잭션을 종료(commit, rollback)하는 코드들이 있기 때문에 코드가 복잡해 지고 중복이 발생한다는 단점이 있었다. 이렇게 중복적인 코드를 하나의
//    메서드로 관리하는 것은 어렵다. 때문에 이러한 중복을 제거하기 위해서 템플릿 콜백 패턴을 적용하면 되는데 템플릿 콜백 패턴을 적용하려면
//    템플릿이 있어야 한다. MVC 1편에서 컨트롤러의 메서드에서 어떠한 반환값이던 어떠한 파리미터던 요청 처리가 되었던 이유가 어댑터 패턴을
//    이용했던것 처럼 템플릿 콜백 패턴도 아무래도 그런식으로 이루어져 있지 않을까 생각이 든다. 아무튼 서비스 단에서 트랜잭선을 시작하는데에 필요한
//    중복을 제거하기위해서 템플릿 콜백 패턴을 이용하고 템플릿 콜백 패턴을 이용하려면 템플릿이 필요한데 스프링에서는 transactionTemplate이라는
//    템플릿을 제공한다. 우리가 지금 할 일은 transactionTemplate을 이용해서 트랜잭션을 적용하는 비즈니스 로직에서 발생하는 중복을 제거해 보자
//    그렇기 위해서는 우선 TransactionTemplate를 필드로 하나 선언을 하고 주입을 받아야 한다.
//    그래서 private final TransactionTemplate txTemplate;을 선언해 줬다.

    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;

//    그리고 TransactionTemplate를 롬복의 @RequiredArgsConstructor을 사용해서 생성자를 만든 후 TransactionTemplate 빈을 주입 받는 것이 아니라
//    트랜잭션 매니저 주입받고 주입받은 트랜잭션 매니저를 이용해서 새로운 TransactionTemplate객체를 생성후 그 객체를 클래스의 TransactionTemplate필드에
//    연결을 할것(MemberServiceV3_2객체의 초기화)이기 때문에 @RequiredArgsConstructor를 주석처리 하고 생성자를 새로 만들었다.
//    => TransactionTemplate를 사용하려면 transactionManager가 필요하다.

    public MemberServiceV3_2(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository){
        this.txTemplate = new TransactionTemplate(transactionManager);
        this.memberRepository = memberRepository;
    }

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        txTemplate.executeWithoutResult((status)->{
            try{
                bizLogic(fromId, toId, money);
            }catch(SQLException e){
                throw new IllegalStateException();
            }
        });

//        트랜잭션 탬플릿안에 있는 excuteWithoutResult메서드 안에서 TransactionStatus status 를 만들어 주기 때문에 다시말해
//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        가 선언되어 있다. excuteWithoutResult의 함수의 트랜잭션 선언(status) 밑에는  아무래도 받아온 람다식을 처리하는 로직이
//        있을것이고 내부적으로 만든 status를 람다 객체 내의 함수에 넣는 로직이 있음으로 바로 아래의 주석처리되어 있는 try와 catch의
//        인지로 넣을 수 있겠다. 아무튼 깊게 이해하지 말고 excuteWithoutResult()메서드 내에서 TransactionStatus status를 선언
//        함으로서 트랜잭션 시작하고 람다의 구현체 부분을 수행하는 비즈니스 로직을 그 다음에 수행한다. 그리고 executeWithoutResult코드가
//        끝났을 무렵에 람다의 비즈니스 로직 코드가 성공적으로 이루어 지면 excuteWithoutResult코드 내에서 커밋을 하고 비즈니스 로직에서
//        예외가 발생하면 executeWithoutResult코드의 끝날 무렵에서 롤백한다.

//        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        try {
//            bizLogic(fromId, toId, money);
//            transactionManager.commit(status);
//        }catch(Exception e){
//            transactionManager.rollback(status);
//            throw new IllegalStateException(e);
//        }
    }

//    TransactionTemplate에 있는 executeWithoutResult함수는 아무래도 아래와 같이 생겼겠다.
//    트랜잭선 시작과 종료후 아무것도 반환하지 않는다.
//    트랜잭션 시작과 종류 후 반환할 값이 필요하다면 executeWithoutResult함수 말고 execute함수를 사용하면 된다.
//    void executeWithoutResult(Consumer<TransactionStatus> action){
//        TransactionStatus ts = this.transactionManager.getTransaction(new DefaultTransactionDefinition());
//        try {
//            action.accept(ts);
//            this.transactionManager.commit(ts);
//        }catch(SQLException e){
//            this.transactionManager.rollback(ts);
//        }
//    }

//    ***********************************************************************************************************************************
//    트랜잭션 템플릿 덕분에 트랜잭션을 시작하고, 커밋하거나 롤백하는 코드가 모두 제거되었다. 트랜잭션 템플릿안에 있는 executeWithoutResult메서드의
//    기본 동작은 다음과 같다. 비즈니스 로직이 정상 수행되면 커밋한다. 물론 비즈니스 로직이 수행되기 전에 먼저 트랜잭션을 만든다.
//    unchecked예외(== runtime 예외)가 발생하면 롤백한다. 그 외의 경우에는 커밋한다.
//       => 비즈니스 로직에서 checked예외가 발생하면 스프링의 기본 룰에 의하여 커밋하는데 이 부분에 대해서는 뒤에서 설명하겠다.
//    비즈니스 로직이 있는 코드에서 예외를 처리하기 위해 try~catch가 들어갔는데,bizLogic()메서드를 호출하면 SQLException 체크 예외를 넘겨준다.
//    SQLException은 checked예외이고 따라서 비즈니스 로직 실행중 예외가 발생하면 커밋된다. 우리는 커밋이 안되고 롤백되게끔 하고 싶기 때문에
//    트랜잭션 템플릿의 executeWithoutResult메서드 안에 있는 비즈니스 로직이 runtime exception(== unchecked exception)이 발생하도록 하면 된다. 따라서
//    비즈니스 로직 내에서 SQLException이 발생하면 runtime exception( == unchecked exception)인 IllegalStateException을 새로 만들어서 던졌다. 그럼
//    executeWithoutResult메서드는 이를 보고 rollback를 하겠다.
//    *************************************************************************************************************************************

    private void bizLogic(String fromId, String toId, int money) throws SQLException{
        Member fromMember = memberRepository.findById(fromId);
        Member toMember = memberRepository.findById(toId);

        memberRepository.update(fromId, fromMember.getMoney()-money);
        validation(toMember);
        memberRepository.update(toId, toMember.getMoney()+money);
    }

    private void validation(Member toMember){
        if(toMember.getMemberId().equals("ex")){
            throw new IllegalStateException("이체중 예외 발생");
        }
    }

}


