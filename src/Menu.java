import java.util.List;
import java.util.Arrays;
import java.util.Scanner;
import java.util.InputMismatchException;

public class Menu {
    private List<String> opcoes;
    private int op;
    private final Scanner scan;

    public Menu(String[] opcoes) {
        this.opcoes = Arrays.asList(opcoes);
        this.op = 0;
        this.scan = new Scanner(System.in);
    }

    public void executa() {
        do {
            this.showMenu();
            this.op = lerOpcao();
        } while (this.op == -1);
    }

    private void showMenu() {
        for (int i = 0; i < this.opcoes.size(); i++) {
            System.out.print(i + 1);
            System.out.print(" - ");
            System.out.println(this.opcoes.get(i));
        }
    }

    private int lerOpcao() {
        int op;

        System.out.print("Opção: ");
        try {
            String aux = "";
            while (aux.isEmpty()) { aux = this.scan.nextLine(); }
            op = Integer.parseInt(aux);
        } catch (InputMismatchException e) {
            op = -1;
        }
        if (op < 0 || op > this.opcoes.size()) {
            System.out.println("Opção Inválida!!!");
            op = -1;
        }
        return op;
    }

    public int getOpcao() {
        return this.op;
    }

    public Scanner getScanner() {
        return this.scan;
    }
}