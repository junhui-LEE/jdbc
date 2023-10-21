package hello.jdbc.repository;

import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.support.JdbcUtils;

import javax.sql.DataSource;
import java.sql.*;
import java.util.NoSuchElementException;

/*
*   JDBC - DataSource 사용, JdbcUtils 사용
* */
@Slf4j
public class MemberRepositoryV2 {
    private final DataSource dataSource;

    public MemberRepositoryV2(DataSource dataSource){
        this.dataSource = dataSource;
    }

    public Member save(Member member) throws SQLException{
        String sql = "insert into member(member_id, money) values(?, ?)";
        Connection con = null;
        PreparedStatement pstmt = null;
        try {
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, member.getMemberId());
            pstmt.setInt(2, member.getMoney());
            pstmt.executeUpdate();
            return member;
        } catch (SQLException e) {
            log.info("db error", e);
            throw e;
        } finally {
            close(con, pstmt, null);
        }
    }
    // JDBC를 통해 이전에 저장한 데이터를 조회하는 기능을 개발해보자.
    public Member findById(String memberId) throws SQLException{
        String sql = "select * from member where member_id = ?";
        Connection con = null;
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try{
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            rs = pstmt.executeQuery();
            if(rs.next()){
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            }else{
                throw new NoSuchElementException("member not found memberId = " + memberId);
            }
        }catch(SQLException e){
            log.error("db error", e);
            throw e;
        }finally{
            close(con, pstmt, rs);
        }
    }
// 2023 10 16 : 추가된 코드
    public Member findById(Connection con, String memberId) throws SQLException{
        String sql = "select * from member where member_id = ?";
        // 원래 이 자리에 Connection con = null; 이 있었고 이 함수 내에서 새로운 커넥션을 가져오는 코드가 있었는데
        // 커넥션을 파라미터로 받기 때문에 삭제한다. (안써준다.)
        // 다른 쿼리를 만약 서비스단에 있는 트랜잭션이 포함시킨다면, 다시말해 이 쿼리와 다른쿼리가 하나의 트랜잭션으로
        // 서비스단에 묶인다면 서비스단에서 그 2개의 쿼리에 같은 커넥션을 파라미터로 넘김으로서 2개의 쿼리는 같은 커넥션을 유지한다.
        PreparedStatement pstmt = null;
        ResultSet rs = null;
        try{
            // con = getConnection; 을 삭제한다.
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1,  memberId);
            rs = pstmt.executeQuery();
            if(rs.next()){
                Member member = new Member();
                member.setMemberId(rs.getString("member_id"));
                member.setMoney(rs.getInt("money"));
                return member;
            }else{
                throw new NoSuchElementException("member not found memberId="+memberId);
            }
        }catch(SQLException e){
            log.error("db error", e);
            throw e;
        }finally {
            JdbcUtils.closeResultSet(rs);
            JdbcUtils.closeStatement(pstmt);
            // JdbcUtils.closeConnection(con); 이것을 씀으로서 connection을 여기서 닫지 않는다. 여기서 connection을
//            닫으면 다른 쿼리에 닫힌 커넥션을 인자로 보내주면 열어진 새로운 커넥션이 들어가기 때문에 결국에는 새로운 커넥션이
//            동일한 트랜잭션의 다른 쿼리에서 생기게 되고 때문에 다른 세션을 쓰게 되고 트랜잭션안에 있는 쿼리들 을 묶어서
//            전부 커밋하거나 전부 롤백하는 것을 하지 못하게 된다.
        }
    }


    //  회원수정 추가
    public void update(String memberId, int money) throws SQLException{
        String sql = "update member set money=? where member_id=?";
        Connection con = null;
        PreparedStatement pstmt = null;
        try{
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            pstmt.executeUpdate();
        }catch(SQLException e){
            log.error("db error", e);
            throw e;
        }finally {
            close(con, pstmt, null);
        }
    }

//    2023 10 17 추가 내용
    public void update(Connection con, String memberId, int money) throws SQLException{
        String sql = "update member set money=? where member_id=?";
        PreparedStatement pstmt = null;
        try{
            pstmt = con.prepareStatement(sql);
            pstmt.setInt(1, money);
            pstmt.setString(2, memberId);
            pstmt.executeUpdate();
        }catch(SQLException e){
            log.error("db error", e);
            throw e;
        }finally{
            JdbcUtils.closeStatement(pstmt);
            // connection은 여기서 닫지 않는다.
        }
    }

    // 회원 삭제 추가
    public void delete(String memberId) throws SQLException{
        String sql = "delete from member where member_id=?";
        Connection con = null;
        PreparedStatement pstmt = null;
        try{
            con = getConnection();
            pstmt = con.prepareStatement(sql);
            pstmt.setString(1, memberId);
            pstmt.executeUpdate();
        }catch (SQLException e){
            log.error("db error", e);
            throw e;
        }finally{
            close(con, pstmt, null);
        }
    }

    private void close(Connection con, Statement pstmt, ResultSet rs) throws SQLException {
        // 스프링은 JDBC를 편리하게 다룰 수 있는 JdbcUtils라는 편의 메서드를 제공한다.
        // JdbcUtils을 사용하면 커넥션을 좀 더 편리하게 닫을 수 있다.
        // JdbcUtils는 java와 DB간의 연결을 도와주는 코드(메서드)들을 담고 있는데 그 중에서 닫는 코드(메서드)를 사용했다.
        // 이전에는 커넥션을 닫는 close()함수를 직접 구현했다.
        JdbcUtils.closeConnection(con);
        JdbcUtils.closeStatement(pstmt);
        JdbcUtils.closeResultSet(rs);
    }

    public Connection getConnection() throws SQLException{
        Connection con = dataSource.getConnection();
        log.info("connection={}, class={}", con, con.getClass());
        return con;
    }

}






































