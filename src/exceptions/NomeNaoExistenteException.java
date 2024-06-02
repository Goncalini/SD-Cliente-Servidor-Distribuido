package exceptions;

public class NomeNaoExistenteException extends Exception{

    public NomeNaoExistenteException(String message) {
        super(message);
    }

    public String getMensagem(){
        return this.getMessage();
    }

}
