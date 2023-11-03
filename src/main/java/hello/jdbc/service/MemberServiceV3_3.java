package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV3;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;

/*
* 트랜잭션 - 트랜잭션 AOP
* */
@Slf4j
@RequiredArgsConstructor
public class MemberServiceV3_3 {
//    private final TransactionTemplate txTemplate;
    private final MemberRepositoryV3 memberRepository;
    public MemberServiceV3_3(PlatformTransactionManager transactionManager, MemberRepositoryV3 memberRepository){
//        this.txTemplate = new TransactionTemplate(transactionManager);
//        트랜잭션 AOP를 적용시킨 프록시가 트랜잭션 관련 코드를 수행하기 떄문에 txTemplate필요 없다.
        this.memberRepository = memberRepository;
    }
    @Transactional
    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
//        나는 accountTransfer메서드가 호출될때 트랜잭션을 걸고 시작하겠다. 이 메서드 호출이 끝나면 성공하면 커밋, 런타입 에러가 터지면 롤백하겠다
//        라는 것이 @Transactional 애노테이션 하나로 끝나는 것이다. 그래서 트랜잭션 txTemplate.executeWithoutResult 코드가 필요 없고 비즈니스 로직 코드만 필요하다.
//        txTemplate.executeWithoutResult((status)->{
//            try{
                bizLogic(fromId, toId, money);
//            }catch(SQLException e){
//                throw new IllegalStateException();
//            }
//        });
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
}


