package exceptions;

public class PalavraPasseExistenteException extends Exception {

    public PalavraPasseExistenteException(String msg){
        super(msg);
    }

    public String getMensagem(){
        return this.getMessage();
    }

}

