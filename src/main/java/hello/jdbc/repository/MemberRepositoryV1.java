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
public class MemberRepositoryV1 {
    private final DataSource dataSource;

    public MemberRepositoryV1(DataSource dataSource){
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
            log.info("db error", e);
            throw e;
        }finally{
            close(con, pstmt, rs);
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
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        }catch(SQLException e){
            log.error("db error", e);
            throw e;
        }finally {
            close(con, pstmt, null);
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
            int resultSize = pstmt.executeUpdate();
            log.info("resultSize = {}", resultSize);
        }catch (SQLException e){
            log.error("db error", e);
            throw e;
        }finally{
            close(con, pstmt, null);
        }
    }

    private void close(Connection con, Statement pstmt, ResultSet rs) throws SQLException {
        // 스프링은 JDBC를 편리하게 다룰 수 있는 JdbcUtils라는 편의 메서드를 제공한다.
        // JdbcUtils을 사용하면 커넥션을 좀 더 편리하게 단을 수 있다.
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






































