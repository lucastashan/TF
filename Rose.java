import java.io.File;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;
import java.util.zip.CRC32;

public class Rose {
  private static final int DATA_SIZE = 1024; 

  public static void main(String[] args) throws IOException, InterruptedException {
    final String TOKEN = "1111";
    final String ACK = "ACK";
    final String NAK = "NAK";

    boolean firstTry = true;
    CRC32 crc = new CRC32();

    // Le o arquivo de conficuracao
    File arquivo = new File("config2.txt");
    Scanner sc = new Scanner(arquivo);

    // Separa IP e porta
    String ipPorta = sc.nextLine();
    String[] parts = ipPorta.split(":");
    String ipVizinho = parts[0];
    int porta = Integer.parseInt(parts[1]);
  
    String apelido = sc.nextLine();
    int segundos = sc.nextInt();
    boolean geraToken = sc.nextBoolean();

    // fila de mensagens
    Queue<String> mensagens = new LinkedList<>();
    mensagens.add("2222;maquinanaoexiste:Rose:Jack:0:Oi Jack!");
    mensagens.add("2222;maquinanaoexiste:Rose:Bob:0:Oi Bob!");  
    mensagens.add("2222;maquinanaoexiste:Rose:Jack:0:Oi Jack 2.");  
    mensagens.add("2222;maquinanaoexiste:Rose:Bob:0:Oi Bob 2.");      
    mensagens.add("2222;maquinanaoexiste:Rose:TODOS:0:Oi pra todos.");      
    

    // cria socket do servidor com a porta 8000
    
    // Declara o socket client
    DatagramSocket clientSocket = new DatagramSocket();
    
    // obtem endereco IP do servidor com o DNS
    InetAddress IPAddress = InetAddress.getByName(ipVizinho);
    
    if( geraToken ) sendPacket(TOKEN, IPAddress, porta);
    
    while (true) {
      
      DatagramSocket serverSocket = new DatagramSocket(6000);
      byte[] receiveData = new byte[1024];
      DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length); 
      // declara o pacote a ser recebido
      serverSocket.receive(receivePacket);

      String dataPacket = new String(receivePacket.getData(), "UTF-8").replaceAll("\\x00*", "");
      // Quando se recebe o token -- pode mandar a mensagem
      if ( dataPacket.equals(TOKEN) ) {
        System.out.println("*TOKEN*");
        TimeUnit.SECONDS.sleep(segundos);

        // Nao tem mensagem para mandar -- passa o token
        // se nao manda a proxima mensagem da fila
        if (mensagens.isEmpty()) {

          sendPacket(TOKEN, IPAddress, porta);
          
        } else {

          // calcula CRC antes de enviar
          String nextMessage = mensagens.peek();
          String[] receive = nextMessage.split(";");
          String[] fields = receive[1].split(":");
          crc.reset();
          crc.update(fields[4].getBytes());
          long erro = crc.getValue();
          fields[3] = Long.toString(erro);

          // insercao de erro
          if(new java.util.Random().nextInt(3) == 0) fields[4] = "*";

          receive[1] = String.join(":", fields);
          nextMessage = String.join(";", receive);

          // avisa se e retransmissao
          if ( !firstTry ) System.out.println("*RETRANSMISSAO*");
          
          sendPacket(nextMessage, IPAddress, porta);

        }
      }
      // Quando se recebe o pacote de fields
      else {
        String[] receive = dataPacket.split(";");
        String[] fields = receive[1].split(":");

        // Se for o destino certo
        if( fields[2].equals(apelido) ) {
          
          // calculo de crc
          crc.reset();
          crc.update(fields[4].getBytes());
          long erro = crc.getValue();
          
          // se a mensagem estiver OK ou nao
          if( Long.parseLong(fields[3]) == erro ) {
            System.out.println(fields[1]+" mandou: "+fields[4]);
            fields[0] = ACK;
          } else fields[0] = NAK;
          
          receive[1] = String.join(":", fields);
          dataPacket = String.join(";", receive);
          
          sendPacket(dataPacket, IPAddress, porta);
             
        }
        // Broadcaast
        else if ( fields[2]. equals("TODOS") ) {
          if( fields[1].equals(apelido) ) {
            mensagens.remove();
            sendPacket(TOKEN, IPAddress, porta);
          } else {
            System.out.println(fields[1]+" mandou: "+fields[4]);
            receive[1] = String.join(":", fields);
            dataPacket = String.join(";", receive);
            sendPacket(dataPacket, IPAddress, porta);
          }
        } 
        // O pacote deu toda volta no anel
        else if ( fields[1].equals(apelido) ) {
          // Mensagem entregue
          if ( fields[0].equals(ACK) ) {
            System.out.println("O pacote foi recebido pelo destino.");
            mensagens.remove();
            sendPacket(TOKEN, IPAddress, porta);            
          }
          // PRECISA SER IMPLEMENTADO
          // Mensagem entregue com erro -- mensagem deve ser retransmitida sem erro um vez
          else if ( fields[0].equals(NAK) ) {
            if ( firstTry ) {
              System.out.println("O pacote foi recebido com erro pelo destino.");
              firstTry = false;
            } else {
              System.out.println("O pacote foi recebido com erro pela segunda vez pelo destino.");
              mensagens.remove();
              firstTry = true;
            }
            sendPacket(TOKEN, IPAddress, porta);
          } else {
            System.out.println("A máquina destino não se encontra na rede ou está desligada.");
            mensagens.remove();
            sendPacket(TOKEN, IPAddress, porta);
          }
        }
        // Se nao for o destino certo
        else sendPacket(dataPacket, IPAddress, porta);
      }
      
      serverSocket.close();

    }
  }

  public static void sendPacket(String buffer, InetAddress address, int port) {
    byte[] bufferBytes = new byte[1024];
    byte[] bufferAux = buffer.getBytes();
    int i = 0;
    for (byte b : bufferAux) {
      bufferBytes[i] = b;
      i++;
    }
    DatagramSocket clientSocket;
    try {
      clientSocket = new DatagramSocket();
      DatagramPacket sendPacket = new DatagramPacket(bufferBytes, bufferBytes.length, address, port);
      clientSocket.send(sendPacket);
    } catch (IOException e) { }    
  }
}
