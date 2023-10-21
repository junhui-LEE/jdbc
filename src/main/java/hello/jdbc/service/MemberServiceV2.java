package hello.jdbc.service;

import hello.jdbc.domain.Member;
import hello.jdbc.repository.MemberRepositoryV1;
import hello.jdbc.repository.MemberRepositoryV2;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

/*
트랜잭션 - 파라미터 연동, 풀을 고려한 종료
 */

@Slf4j
@RequiredArgsConstructor
public class MemberServiceV2 {
    private final DataSource dataSource;
    private final MemberRepositoryV2 memberRepository;

    public void accountTransfer(String fromId, String toId, int money) throws SQLException {
        Connection con = dataSource.getConnection();
        try {
            con.setAutoCommit(false); // 이렇게 하면 set autocommit false를 DB에 날려준다. 여기서부터 트랜잭션 시작이다.
            // 비즈니스 로직
            bizLogic(con, fromId, toId, money);
            // 성공시 커밋!
            con.commit();
//        => 비즈니스 로직 실행중 실패해서 예외가 발생하면 con.commit();이 실행되지 않고 아래의 catch문이 예외를 잡고 트랜잭션안의
//            전체 쿼리를 롤백 시킨다.
        }catch(Exception e){
            con.rollback(); // 실패시 롤백
            throw new IllegalStateException(e);
        }finally{
//            커밋이던 롤백이던 해서 하나의 트랜잭션이 끝났으면 커넥션을 닫는다
            release(con);
        }
    }

    private void bizLogic(Connection con, String fromId, String toId, int money) throws SQLException{
        Member fromMember = memberRepository.findById(con, fromId);
        Member toMember = memberRepository.findById(con, toId);

        memberRepository.update(con, fromId, fromMember.getMoney()-money);
        validation(toMember);
        memberRepository.update(con, toId, toMember.getMoney()+money);
    }

    private void validation(Member toMember){
        if(toMember.getMemberId().equals("ex")){
            throw new IllegalStateException("이체중 예외 발생");
        }
    }

    private void release(Connection con){
        if(con != null){
            try {
                con.setAutoCommit(true);  // 커넥션 풀 고려
                con.close();              // 커넥션 풀에 커넥션 반납
//            finally{ .. }를 사용해서 커넥션을 모두 사용하고 나면 안전하게 종료한다. 그런데 커넥션 풀을 사용하면 con.close()를 호출했을때
//            커넥션이 종료되는 것이 아니라 풀에 반납된다. 현재 수동 커밋 모드로 동작하기 때문에 커넥션을 풀에 돌려주기 전에 기본 값인 자동 커밋 모드로
//            변경하는 것이 안전하다. 대부분 자동커밋 모드라고 가정을 하기 때문에 의도치 않게 문제가 발생할 수 있다. 커넥션 풀을 사용하지 않으면
//            커넥션 종료후 새로운 커넥션 생성시 DB에서 자동커밋 모드로 동작한다. DB에서 그렇게 서팅이 되어 있다.
            }catch(Exception e){
                log.info("error", e);
            }
        }
    }
}

// service 단에서 DataSource로 부터 커넥션을 얻은 후에 그 커넥션을 가지고 우선 service단에서  autocommit을 false로 설정하고
// 그 다음에 service단에서 각각의 쿼리를 보내는 Repository안의 메서드의 인자로 커넥션을 넘겨주고 service단에서 오류가 발생시에 rollback하고
// 오류가 발생 안하면 commit한다. 그리고 service단에서 커넥션을 끊는다.


































