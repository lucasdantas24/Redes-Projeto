import java.io.IOException;
import java.net.*;
import java.util.*;

public class Sender {

    public static boolean timeout;

    public final static int WINDOW = 10;

    public final static int TIMER = 10000;

    public static ArrayList<Mensagem> pacotesDaJanela = new ArrayList<>();

    private static int nextSeqNum = 0;

    public static int getNextSeqNum() {
        return nextSeqNum;
    }

    public static void addNextSeqNum() {
        nextSeqNum++;
    }

    private static int sendBase = 0;

    public static int getSendBase() {
        return sendBase;
    }

    public static void changeSendBase(int ackNumber) {
        sendBase = Math.max(ackNumber, sendBase);
    }

    private static InetAddress IPAdress;

    static {
        try {
            IPAdress = InetAddress.getByName("127.0.0.1");
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }
    }

    public static InetAddress getIPAdress() {
        return IPAdress;
    }

    public static ArrayList<Mensagem> pacotesEnviados = new ArrayList<>();

    public static ArrayList<DatagramPacket> pacotesForaDeOrdem = new ArrayList<>();

    public static void main(String[] args) throws Exception {

        while (true) {
            //Recebendo mensagem a ser enviada
            Scanner entrada = new Scanner(System.in);
            String dataUser = entrada.next();
            //Se tiver espaço disponível na janela, pacote é autorizado
            if (pacotesDaJanela.size() < WINDOW) {
                int id;

                //Recebendo mensagem e transformando para bytes a serem enviados na mensagem

                byte[] data = dataUser.getBytes();

                //Menu
                System.out.println(
                        "Qual será a forma de envio? (Escreva da forma que está dentro dos colchetes)");

                System.out.println("[Lento],[Perda],[Fora_de_Ordem],[Duplicada],[Normal]");

                //Recebe a forma de envio e define o ID da mensagem
                String escolha = entrada.next();
                id = nextSeqNum;

                //Print mostrando informações da mensagem enviada
                char aspas = 34;
                System.out.println("Mensagem " + aspas + dataUser + aspas +
                        " enviada como [" + escolha + "] com id " + id);

                //Forma pacote com a mensagem e os adiciona ao buffer
                Mensagem mensagem = new Mensagem(id, data);
                byte[] sndPacket = mensagem.mensagemParaString().getBytes();
                pacotesEnviados.add(mensagem);
                pacotesDaJanela.add(mensagem);
                DatagramPacket sendPacket = new DatagramPacket(sndPacket, sndPacket.length, IPAdress, 9000);

                //Cria Thread e a inicia para envios consecutivos
                if (!escolha.equals("Fora_de_Ordem") & pacotesForaDeOrdem.size() < 9) {
                    ThreadEnvio te = new ThreadEnvio(escolha, sendPacket, mensagem);
                    te.start();
                } else {
                    pacotesForaDeOrdem.add(sendPacket);
                    Sender.addNextSeqNum();
                }
            }
            //Se não há espaço na janela, pacote é não autorizado
        }
    }

    static class ThreadEnvio extends Thread {

        private final String escolha;
        private final DatagramPacket sendPacket;
        private final Mensagem mensagem;

        public ThreadEnvio(String escolha, DatagramPacket sendPacket, Mensagem mensagem) {
            this.escolha = escolha;
            this.sendPacket = sendPacket;
            this.mensagem = mensagem;
        }

        @Override
        public void run() {
            try {
                //Criando Socket para envio
                DatagramSocket clientSocket = new DatagramSocket();

                if (Sender.getNextSeqNum() - Sender.getSendBase() < Sender.WINDOW) {

                    if (Objects.equals(escolha, "Lento")) {

                        //Thread iniciada para mandar com atraso
                        ThreadAtraso thread = new ThreadAtraso(sendPacket);
                        thread.start();

                        Sender.addNextSeqNum();

                    } else if (!escolha.equals("Perda")) {

                        // Manda o pacote de forma normal
                        clientSocket.send(sendPacket);

                        if (escolha.equals("Duplicada")) {
                            // Manda o pacote duplicado
                            clientSocket.send(sendPacket);
                        }

                        Sender.addNextSeqNum();
                    } else {
                        //Não manda o pacote, pois a escolha foi Perda
                        Sender.addNextSeqNum();
                    }
                }

                if (Sender.pacotesDaJanela.get(0).equals(mensagem)) {
                    ThreadTimer tt = new ThreadTimer(mensagem);
                    tt.start();
                }

                byte[] ackBytes = new byte[1024];

                DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);

                //Socket espera receber resposta até dar timeout caso ele seja o mais antigo sem resposta ou
                //caso ele tenha sido reenviado após o timeout do mais antigo.
                while (!Sender.timeout | !Sender.pacotesDaJanela.get(0).equals(mensagem)) {
                    try {
                        //Tenta receber resposta ack do Receiver
                        clientSocket.setSoTimeout(100);
                        clientSocket.receive(ack);

                        //Trata resposta
                        int ackNumber = Integer.parseInt(new String(ack.getData(), ack.getOffset(), ack.getLength()));
                        System.out.println("Mensagem id " + (ackNumber - 1) + " recebida pelo receiver.");
                        Sender.changeSendBase(ackNumber);
                        Sender.pacotesDaJanela.remove(mensagem);

                        if (Sender.pacotesForaDeOrdem.size() > 0) {
                            ThreadForaDeOrdem tfo = new ThreadForaDeOrdem(Sender.pacotesForaDeOrdem.get(0));
                            tfo.start();
                        }

                        if (Sender.pacotesDaJanela.size() > 0) {
                            ThreadTimer tt = new ThreadTimer(Sender.pacotesDaJanela.get(0));
                            tt.start();
                        }

                        return;
                    } catch (SocketTimeoutException | SocketException e) {
                        if (Sender.pacotesDaJanela.size() == 0) return;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //Lista para reenvio de pacotes que não receberam  resposta ACK
                System.out.println("IDs reenviados: ");
                for (int i = Sender.getSendBase(); i < Sender.getNextSeqNum(); i++)
                    System.out.println(Sender.pacotesEnviados.get(i).getNumSeq());

                //Reenvio de pacote 1 a 1
                for (int i = Sender.getSendBase(); i < Sender.getNextSeqNum(); i++) {

                    Mensagem msg = Sender.pacotesEnviados.get(i);
                    byte[] data2 = Sender.pacotesEnviados.get(i).mensagemParaString().getBytes();

                    //Reenvio de pacote
                    DatagramPacket packet = new DatagramPacket(data2, data2.length, Sender.getIPAdress(), 9000);
                    clientSocket.send(packet);

                    //Recebimento do ACK, como o envio foi normal não tem timeout
                    clientSocket.receive(ack);
                    int ackNumber = Integer.parseInt(new String(ack.getData(), ack.getOffset(), ack.getLength()));
                    System.out.println("Mensagem id " + (ackNumber - 1) + " recebida pelo receiver.");
                    Sender.changeSendBase(ackNumber);
                    Sender.pacotesDaJanela.remove(msg);
                }
                Sender.timeout = false;
                if (Sender.pacotesDaJanela.size() > 0) {
                    ThreadTimer tt = new ThreadTimer(Sender.pacotesDaJanela.get(0));
                    tt.start();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    static class ThreadForaDeOrdem extends Thread {
        private final DatagramPacket sendPacket;

        public ThreadForaDeOrdem(DatagramPacket sendPacket) {
            this.sendPacket = sendPacket;
        }

        @Override
        public void run() {
            try {
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.send(sendPacket);
                String msg = new String(sendPacket.getData(), sendPacket.getOffset(), sendPacket.getLength());
                Mensagem mensagem = Mensagem.stringParaMensagem(msg);

                byte[] ackBytes = new byte[1024];

                DatagramPacket ack = new DatagramPacket(ackBytes, ackBytes.length);

                //Tenta receber resposta ack do Receiver
                clientSocket.receive(ack);

                //Trata resposta
                int ackNumber = Integer.parseInt(new String(ack.getData(), ack.getOffset(), ack.getLength()));
                System.out.println("Mensagem id " + (ackNumber - 1) + " recebida pelo receiver.");
                Sender.changeSendBase(ackNumber);
                Sender.pacotesDaJanela.remove(mensagem);

                Sender.pacotesForaDeOrdem.remove(sendPacket);

                if (Sender.pacotesDaJanela.size() > 0) {
                    ThreadTimer tt = new ThreadTimer(Sender.pacotesDaJanela.get(0));
                    tt.start();
                }


                for (int i = Sender.getSendBase(); i < Sender.getNextSeqNum(); i++) {

                    Mensagem m = Sender.pacotesEnviados.get(i);
                    byte[] data2 = Sender.pacotesEnviados.get(i).mensagemParaString().getBytes();

                    //Reenvio de pacote
                    DatagramPacket packet = new DatagramPacket(data2, data2.length, Sender.getIPAdress(), 9000);
                    clientSocket.send(packet);

                    //Recebimento do ACK, como o envio foi normal não tem timeout
                    clientSocket.receive(ack);
                    ackNumber = Integer.parseInt(new String(ack.getData(), ack.getOffset(), ack.getLength()));
                    System.out.println("Mensagem id " + (ackNumber - 1) + " recebida pelo receiver.");
                    Sender.changeSendBase(ackNumber);
                    Sender.pacotesDaJanela.remove(m);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    static class ThreadAtraso extends Thread {

        private final DatagramPacket datagramPacket;

        public ThreadAtraso(DatagramPacket datagramPacket) {
            this.datagramPacket = datagramPacket;
        }

        @Override
        public void run() {
            try {
                //Thread aguarda 11 segundos e envia pacote
                Thread.sleep(11000);
                DatagramSocket clientSocket = new DatagramSocket();
                clientSocket.send(datagramPacket);
            } catch (IOException | InterruptedException e) {
                e.printStackTrace();
            }

        }
    }
}