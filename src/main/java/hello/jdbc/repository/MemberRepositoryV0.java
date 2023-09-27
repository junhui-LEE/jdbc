package hello.jdbc.repository;

import hello.jdbc.connection.DBConnectionUtil;
import hello.jdbc.domain.Member;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;

@Slf4j
public class MemberRepositoryV0 {
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
    private void close(Connection con, Statement pstmt, ResultSet rs){
        if(rs != null){
            try{
                rs.close();
            }catch(SQLException e){
                log.info("error", e);
            }
        }
        if(pstmt != null){
            try{
                pstmt.close();
            }catch(SQLException e){
                log.info("error", e);
            }
        }
        if(con != null){
            try{
                con.close();
            }catch(SQLException e){
                log.info("error", e);
            }
        }
    }
    private Connection getConnection () {
        return DBConnectionUtil.getConnection();
    }
}



































