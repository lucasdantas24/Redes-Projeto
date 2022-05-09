//Thread para simular temporizador
public class ThreadTimer extends Thread {
    private final Mensagem mensagem;

    public ThreadTimer(Mensagem mensagem) {
        this.mensagem = mensagem;
    }

    @Override
    public void run() {
        try {
            sleep(Sender.TIMER);
            if (Sender.pacotesDaJanela.size() > 0) {
                if (Sender.pacotesDaJanela.get(0).equals(mensagem))
                    Sender.timeout = true;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
}
