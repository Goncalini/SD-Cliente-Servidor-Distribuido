package exceptions;

public class NomeExistenteException extends Exception{

    public NomeExistenteException(String message) {
        super(message);
    }

    public String getMensagem(){
        return this.getMessage();
    }

}
