import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.ArrayList;

public class Receiver {
    public static void main(String[] args) {
        try {
            DatagramSocket serverSocket = new DatagramSocket(9000);

            ArrayList<Mensagem> pacotesRecebidos = new ArrayList<>();
            int sendBase = 0;

            byte[] recBuffer = new byte[1024];

            while (true) {
                // Recebendo o pacote
                DatagramPacket pacoteRecebido = new DatagramPacket(recBuffer, recBuffer.length);
                serverSocket.receive(pacoteRecebido);
                String information = new String(
                        pacoteRecebido.getData(),
                        pacoteRecebido.getOffset(),
                        pacoteRecebido.getLength());
                Mensagem receivedMessage = Mensagem.stringParaMensagem(information);

                boolean duplicado = false;
                //Verifica se é duplicado
                for (Mensagem m:pacotesRecebidos) {
                    if (m.getNumSeq() == receivedMessage.getNumSeq()) {
                        duplicado = true;
                        break;
                    }
                }
                //Manda mensagem de duplicação
                if (duplicado) {
                    System.out.println(
                            "Mensagem id "
                            + receivedMessage.getNumSeq()
                            + " recebida de forma duplicada.");
                } else if (receivedMessage.getNumSeq() == sendBase) {
                    //Mensagem esperada, guarda no Buffer
                    sendBase++;
                    pacotesRecebidos.add(receivedMessage);
                    System.out.println(
                            "Mensagem id "
                            + receivedMessage.getNumSeq()
                            + " recebida na ordem, entregando para a camada de aplicação.");
                }else {
                    //Fora de ordem, descarta e mostra pacotes faltantes
                    System.out.print(
                            "Mensagem id "
                            + receivedMessage.getNumSeq()
                            + " recebida fora de ordem, ainda não recebidos os identificadores: ");
                    for (int i = sendBase; i < receivedMessage.getNumSeq(); i++) {
                        if (i < receivedMessage.getNumSeq() - 1) {
                            System.out.print(i + ", ");
                        } else {
                            System.out.println(i);
                        }
                    }
                }

                //Envio do ACK

                String ackPacote = String.valueOf(sendBase);
                byte[] ackBytes = ackPacote.getBytes();

                DatagramPacket ackPacket = new DatagramPacket(
                        ackBytes,
                        ackBytes.length,
                        pacoteRecebido.getAddress(),
                        pacoteRecebido.getPort());
                // Manda o ACK
                serverSocket.send(ackPacket);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
