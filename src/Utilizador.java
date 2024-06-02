public class Utilizador {

    private String password;
    private String nome;

    public String getpassword() {
        return password;
    }

    public void setpassword(String password) {
        this.password = password;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public Utilizador(String nome, String password) {
        this.setNome(nome);
        this.setpassword(password);
    }

    public Utilizador(Utilizador utilizador){
        this.nome = utilizador.getNome();
        this.password = utilizador.getpassword();
    }
    public Utilizador clone(){
        return new Utilizador(this);
    }

    @Override
    public String toString() {
        return "Utilizador{" +
                "Password='" + password + '\'' +
                ", Nome='" + nome + '\'' +
                '}';
    }
}
