import java.io.IOException;
import java.util.Scanner;

public class ClientMain {

    public static void main(String[] args) throws IOException {
        Scanner scan = new Scanner(System.in);
        new Client(scan).run();
    }
}