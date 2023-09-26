package hello.jdbc.connection;

import org.junit.jupiter.api.Test;

import java.sql.Connection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class DBConnectionUtilTest {
    @Test
    void connection(){
        Connection connection = DBConnectionUtil.getConnection();
        assertThat(connection).isNotNull();
    }
}
