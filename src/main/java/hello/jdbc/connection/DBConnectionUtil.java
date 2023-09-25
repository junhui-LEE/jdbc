package hello.jdbc.connection;

import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

import static hello.jdbc.connection.ConnectionConst.*;

@Slf4j
public class DBConnectionUtil {
    public static Connection getConnection(){
        try{
            Connection connection = DriverManager.getConnection(URL, USERNAME, PASSWORD);
            /*
            * static import 하는법 : 우선 해당클래스를 선택한 다음에 alt+enter을 한다음에
            *                       Add on-demand static import ~~ 라고 쓰여 있는것 누르면 된다.
            * */
            log.info("get connection={}, class={}", connection, connection.getClass());
            return connection;
        }catch(SQLException e){
            throw new IllegalStateException(e);
        }

    }
}
