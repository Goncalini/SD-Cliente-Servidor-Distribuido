import exceptions.NomeExistenteException;
import exceptions.PalavraPasseNaoExistenteException;
import exceptions.NomeNaoExistenteException;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class GestaoUtilizador {

    private Map<String,Utilizador> utilizadores; //coleçao de todos os utilizadores criados
    private final ReadWriteLock usersLock = new ReentrantReadWriteLock();
    public GestaoUtilizador(){
        this.utilizadores = new HashMap<>();
    }

    public Map<String, Utilizador> getUtilizadores() {
        Map<String,Utilizador> newMap= new HashMap<>();
        for(Map.Entry<String,Utilizador> m : utilizadores.entrySet())
            newMap.put(m.getKey(),m.getValue().clone());
        return newMap;
    }

    public void setUtilizadores(Map<String, Utilizador> utilizadores) {
        this.utilizadores = new HashMap<>();
        for(String l : utilizadores.keySet())
            this.utilizadores.put(l,utilizadores.get(l).clone());
    }

    public Utilizador getUtilizadorNome(String nome) throws NomeNaoExistenteException{
        Utilizador u = null;
        for (Map.Entry<String, Utilizador> entry : this.utilizadores.entrySet()) {
            if((entry.getValue().getNome()).equals(nome)) {
                u = entry.getValue();
                break;
            }
        }
        if(u == null){
            throw new NomeNaoExistenteException("Este email não existe!");
        }
        return u.clone();
    }

    public boolean getUtilizadorNomePasse(String nome,String passe){
        Utilizador u = null;
        String p = null;
        for (Map.Entry<String, Utilizador> entry : this.utilizadores.entrySet()) {
            if(((Objects.equals(entry.getValue().getpassword(), passe)) && (Objects.equals(entry.getValue().getNome(), nome)))){
                return true;
            }
        }
        return false;
    }

    public String verificaNome(String nome) throws NomeExistenteException {
        if(this.utilizadores == null){
            return nome;
        }
        String Nome = "";
        for (Map.Entry<String, Utilizador> entry : this.utilizadores.entrySet()) {
            if((entry.getValue().getNome()).equals(nome)) {
                Nome = entry.getValue().getNome();
                break;
            }
        }
        if(Nome.equals(nome)){
            throw new NomeExistenteException("Este nome já existe!");
        }
        return nome;
    }

    public boolean verificaPasse(String passe){
        if(this.utilizadores == null){
            return false;
        }
        String Passe = "";
        for (Map.Entry<String, Utilizador> entry : this.utilizadores.entrySet()) {
            if((entry.getValue().getpassword()).equals(passe)) {
                return true;
            }
        }
        return false;
    }

    public void adicionaUtilizador(Utilizador bla){
        this.utilizadores.put(bla.getNome(),bla);
    }

    public void adicionaUtilizador(String username, String password){
        try{
            this.usersLock.writeLock().lock();
            Utilizador user = new Utilizador(username,password);
            this.utilizadores.put(username,user);
        } finally {
            this.usersLock.writeLock().unlock();
        }
    }

}

