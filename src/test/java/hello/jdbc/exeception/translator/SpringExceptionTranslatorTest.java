package hello.jdbc.exeception.translator;

import hello.jdbc.connection.ConnectionConst;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.support.SQLErrorCodeSQLExceptionTranslator;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import javax.sql.DataSource;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;
import static org.assertj.core.api.Assertions.*;


@Slf4j
public class SpringExceptionTranslatorTest {
    DataSource dataSource; // DataSource인테페이스는 JPA접근 로직이 담긴 클래스와 JDBC접근 로직이 담긴 클래스 모두에 리소스로 쓰인다.

    @BeforeEach
    void init(){
        dataSource = new DriverManagerDataSource(URL, USERNAME, PASSWORD);
    }

    @Test
    void sqlExceptionErrorCode() throws SQLException{
        String sql = "select bad grammer";
        try{
            Connection con = dataSource.getConnection();
            PreparedStatement stmt = con.prepareStatement(sql);
            stmt.executeQuery();
        }catch(SQLException e){
//            assertThat(e.getErrorCode()).isEqualTo(42122);
//            int errorCode = e.getErrorCode();
//            log.info("errorCode={}", errorCode);
//            log.info("error", e);
//            => 이전에 살펴봤던 SQL ErrorCode를 직접 확인하는 방법이다. 이렇게 직접 예외를 확인하고 하나하나
//               스프링이 만들어준 예외로 변환하는 것은 현실성이 없다. 이렇게 하려면 해당 오류 코드를 확인하고
//               스프링의 예외 체계에 맞추어 예외를 직접 변환해야 할 것이다. 그리고 데이터베이스마다 오류 코드가
//               다르다는 점도 해결해야 한다. 그리서 스프링은 예외 변환기를 제공한다.
            assertThat(e.getErrorCode()).isEqualTo(42122);
            SQLExceptionTranslator exTranslator = new SQLErrorCodeSQLExceptionTranslator(dataSource);
            DataAccessException resultEx = exTranslator.translate("select", sql, e);
            log.info("resultEx", resultEx);
            assertThat(resultEx.getClass()).isEqualTo(BadSqlGrammarException.class);
//          우리가 repository에서 예외 변환을 우리가 직접하는 것이 아니고 이 코드를 직접 써서 변환하면
//          되는 것이다. 그럼 스프링의 예외 추상화 계층에 있는 스프링 예외 추상화를 그대로 쓸 수 있는 것이다.
//          ,다시말해 스프링에서 제공하는 데이터 접근 계층의 예외를 쓸 수 있는 것이다.
        }
    }

}
