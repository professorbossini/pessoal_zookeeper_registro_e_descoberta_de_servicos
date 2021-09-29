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
        //usamos a porta 10000 por padrão, caso o usuário não informe uma
        //o número 10000 é arbitrário
        int porta = args.length >= 1 ? Integer.parseInt(args[0]): 10000;
        Principal principal = new Principal();
        ZooKeeper zooKeeper = principal.conectar();
        RegistroDeServicos registroDeServicos = new RegistroDeServicos(zooKeeper);
        EleicaoCallback eleicaoCallback = new EleicaoCallbackImpl(
                registroDeServicos,
                porta
        );
        EleicaoDeLider eleicaoDeLider = new EleicaoDeLider(
                zooKeeper,
                eleicaoCallback
        );
        eleicaoDeLider.realizarCandidatura();
        eleicaoDeLider.eleicaoEReeleicaoDeLider();
        principal.executar();
        principal.fechar();
    }



}
