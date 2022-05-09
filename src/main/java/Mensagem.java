import com.google.gson.Gson;

import java.io.Serializable;

public class Mensagem implements Serializable {

    private final int numSeq;

    private byte[] data;

    public Mensagem(int numSeq, byte[] data) {
        this.numSeq = numSeq;
        this.data = new byte[1024];
        this.data = data;
    }

    public int getNumSeq() {
        return numSeq;
    }

    public byte[] getData() {
        return data;
    }

    public String mensagemParaString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public static Mensagem stringParaMensagem(String jsonString) {
        Gson gson = new Gson();
        return gson.fromJson(jsonString, Mensagem.class);
    }

}
