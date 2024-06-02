import java.io.IOException;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        System.out.println("Server is running...");
        new Server().run();
    }
}