package exceptions;

public class PalavraPasseNaoExistenteException extends Exception{

    public PalavraPasseNaoExistenteException(String message) {
        super(message);
    }

    public String getMensagem(){
        return this.getMessage();
    }

}
