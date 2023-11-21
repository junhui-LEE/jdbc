package hello.jdbc.repository.ex;

public class MyDbException extends RuntimeException{
// MyDbException은 런타임(언체크)예외이다.
    public MyDbException() {
    }

    public MyDbException(String message) {
        super(message);
    }

    public MyDbException(String message, Throwable cause) {
        super(message, cause);
    }

    public MyDbException(Throwable cause) {
        super(cause);
    }

}
