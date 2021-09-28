import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

public class Principal {
    public static final String HOST = "localhost";
    public static final String PORTA = "2181";
    public static final int TIMEOUT = 5000;
    public static final String ELEICAO = "/eleicao";
    private ZooKeeper zooKeeper;

    public ZooKeeper conectar () throws IOException {
        this.zooKeeper = new ZooKeeper(
                String.format("%s:%s", HOST, PORTA),
                TIMEOUT,
                (event) -> {
                    switch (event.getType()){
                        case None:
                            if (event.getState() == Watcher.Event.KeeperState.SyncConnected){
                                System.out.println("Conectou!");
                            }
                            else{
                                synchronized (zooKeeper){
                                    System.out.println ("Desconectou");
                                    zooKeeper.notify();
                                }
                            }
                    }
                }
        );
        return this.zooKeeper;
    }

    public void fechar () throws InterruptedException{
        this.zooKeeper.close();
    }

    public void executar () throws InterruptedException{
        synchronized (zooKeeper){
            zooKeeper.wait();
        }
    }

    public static void main(String[] args) throws InterruptedException, IOException, KeeperException {
        Principal principal = new Principal();
        EleicaoDeLider eleicaoDeLider = new EleicaoDeLider(principal.conectar());
        eleicaoDeLider.realizarCandidatura();
        eleicaoDeLider.eleicaoEReeleicaoDeLider();
        principal.executar();
        principal.fechar();
    }



}
