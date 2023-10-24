package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV2;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.DefaultTransactionDefinition;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/*
트랜잭션 - 트랜잭션 매니저

이전시간에는 DataSourceUtils.getConnection(dataSource); 와 DataSourceUtils.releaseConnection(con, dataSource);를 이용해서
MemberRepositoryV3를 리팩토링 함으로서 MemberRepositoryV3가 트랜잭션 동기화 매니저와 상호작용을 하도록 구현을 했다. 다시한번 정리해 보자면
서비스에서 트랜잭션을 시작하려고 하면 트랜잭션 매니저에서 dataSource로 부터 커낵션을 가져온 다음에 그 커넥션을 트랜잭션 동기화 매니저에게 주고
리포지토리는 트랜잭션 동기화 매니저와 상호작용하면서 서비스의 입장에서 트랜잭션을 시작할지 말지의 유무, 즉, 트랜잭션 매니저로부터 트랜잭션 동기화 매니저
에게 커넥션을 줬는지에 대한 유무, 다시말해, 트랜잭션 동기화 매니저가 관리하는 커넥션이 있는지에 대한 유무에 따라서 다른 커넥션을 가져오고 , 정리하면
트랜잭션 동기화 매니저가 관리하는 커넥션이 있을경우 그 커넥션을 가져오고 트랜잭션 동기화 매니저가 관리하는 커넥션이 없을 경우 새로운 커넥션을 생성한
다음에 그 새로운 커넥션을 사용한다. 그리고 이때 사용하는 메서드가 DataSourceUtils.getConnection(dataSource);이다.
DataSourceUtils.getConnection(dataSource) 가 리포지토리에서 트랜잭션 동기화 매니저를 쓰는 코드이다. 다시말해 DataSourceUtils.getConnection(dataSource)
가 트랜잭션 동기화 매니저 코드이다. DataSourceUtils.releaseConnection(con, dataSource);도 마찬가지로 트랜잭션 동기화 매니저 코드이고
서비스단에서 트랜잭션을 사용하기위해서 트랜잭션 매니저로부터 트랜잭션 동기화 매니저에게 전달된 커넥션이면 커넥션을 닫지않고 리포지토리에서 새롭게
생성한 커넥션이면 바로 닫는다.

참고로 다시한번 여기에 타자를 치면서 생각을 정리해 보자. 뭐를 정리할 것이냐면, 트랜잭션 매니저가 무엇인가에 대한 정리이다. 서비스단에서
트랜잭션을 적용시키기 위해서 코드를 구현하면 con.setAutoCommit(false)라던지 뭐시기 뭐시기 다른 코드들로 인해서 서비스단의 코드가
복잡해 진다. 그리고 만일 DB접근 기술을 JDBC에서 JPA로 바꾼다면 리포지토리에서 발생하는 예외가 서비스단 까지 올라오기 떄문에 서비스 단에서
해당 DB접근 기술에 의존한다는 단점도 있다. 서비스단은 순수하고 변하지 않는 java코드로 구성하는 것이 가장 좋다. 때문에 이러한 문제점들을
해결하기 위해서 트랜잭션 매니저 라는 것을 사용한다. DB접근기술들은 이것(트랜잭션 매니저)을 표준으로 삼고 구현되어 있다. 개발자는 서비스단에서
트랜잭션 매니저를 사용하면 DB접근이 무었이던지 간에 순수하고 변하지 않는 java코드를 서비스단에 코딩할 수 있다. 트랜잭션 매니저는 dataSource로
부터 얻어온 커넥션을 트랜잭션 동기화 매니저 에게 반납하는데, 이렇게 하기 때문에 서비스단에서 트랜잭션을 사용하려고 하면 원자단위 안에 있는
쿼리들이 트랜잭션 동기화 매니저가 동기화를 해 주기 때문에 모두 실행되거나 모두 실패하거나 할 수 있게 된다. 왜냐하면 리포지토리에 있는
각각의 쿼리들은 DataSourceUtils.getConnection(dataSource)라는 코드로 커넥션을 얻어오는데 DataSourceUtils.getConnection(dataSource);
코드가 트랜잭션 동기화 매니저 코드이고 , 이 코드로 인해서 커넥션을 공유해서 쓰기 때문이다.

* 그럼 앞으로 우리가 할 일은 서비스단에서 트랜잭션 매니저를 통해서 트랜잭션 동기화 매니저에게 트랜잭션의 적용 유무에 따라서 dataSource로 부터 얻은 커넥션을 넘겨 주거나 안넘겨 주거나 하는 것이다. *
*참고로 트랜잭션 매니저와 트랜잭션 동기화 매니저는 같은(동일한) dataSource를 쓴다. => dataSource를 공유한다. *

*/

@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_1 {
//    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;
//    기존의 dataSource를 주입받는 코드를 제거하고 DB접근 기술이 어떤것이던지 간에 순수하고 변하지 않는 java코드로 트랜잭션이 적용되어 있는
//    서비스단을 코딩하기 위해서 순수 java코드로 되어 있는 트랜잭션 매니저인 PlatformTransactionManager을 자료형으로 하는 변수를
//    선언하고 DI로 주입받아야 한다. => 나는 jdbc에 의존하는 트랜잭션 구현 객체(jdbc 트랜잭션 매니저)를 주입받을 것이다.
//    참고로 new DataSourceTransactionManager은 jdbc와 관련된 트랜잭션 매니저 구현체이고 new JpaTransactionManager은 JPA와 관련된
//    트랜잭션 매니저 구현체 이다. 기본적으로 이 둘은 스프링이 제공을 하고 JpaTransactionManager을 gradle에서 가져와서 라이브러리에 추가되면
//    ( springboot-starter-JpaTransactionManager 이겠지? )JpaTransactionManager을 쓸 수 있다. 나는 지금 JpaTransactionManager가 없다.
    private final MemberRepositoryV3 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {

//        Connection con = dataSource.getConnection();
        TransactionStatus status = transactionManager.getTransaction(new DefaultTransactionDefinition());
//        주입받은 트랜잭션 매니저에 트랜잭션과 관련된 옵션, 여기서는 new DefaultTransactionDefinition(),을 설정하고 트랜잭션 상태를 만든다.
//        이 코드가 "트랜잭션을 시작"하는 코드이다. => DB에 set autocommit false를 날려 준다.
//        트랜잭션 동기화 매니저와 공유하는 dataSource를 주입받은 트랜잭션 매니저가 dataSource로부터 커넥션을 가져오고 그 커넥션에 set autocommit false
//        를 설정하고 트랜잭션 동기화 매니저에게 커넥션을 준다.
        try {
//            con.setAutoCommit(false);
            // "비즈니스 로직"
            bizLogic(fromId, toId, money);
//            con.commit();
            transactionManager.commit(status);
            // 정상적으로 계좌이체 로직이 수행되면, bizLogic();코드에서 예외가 발생하지 않으면 트랜잭션 매니저가 커밋을 해서 트랜잭션을 종료한다.
        }catch(Exception e){
//            con.rollback();
            transactionManager.rollback(status);
            // bizLogic(), 계좌이체, 수행중 예외가 발생하면 트랜잭션 매니저가  롤백을 해서 트랜잭션을 종료한다.
            throw new IllegalStateException(e);
            // 그리고나서 예외를 던진다.
        }
//        finally{
//            release(con);
//        }
//        트랜잭션 매니저가 커밋이나 롤백을 해서 하나의 트랜잭션이 종료되면 트랜잭션 매니저가 release를 알아서 해준다. 따라서 release코드가 필요 없다.
//        잊어먹어서 release가 했던 역할을 되새겨 보자면 MemberServiceV2에 사용되었던 release는 커넥션 풀을 고려해서 우선은 해당 커넥션에
//        con.setAutoCommit(true); 로 설정해서 autocommit가 일어나도록 설정했고 con.close()를 사용해서 커넥션 풀에 커넥션을 반납했다.
    }

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
//    private void release(Connection con){
//        if(con != null){
//            try {
//                con.setAutoCommit(true);
//                con.close();
//            }catch(Exception e){
//                log.info("error", e);
//            }
//        }
//    }
}


