import org.apache.zookeeper.*;
import org.apache.zookeeper.data.Stat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RegistroDeServicos {

    private String zNodeAtual;
    private List <String> enderecos;
    private static final String REGISTRO_ZNODE = "/registro_de_servicos";
    private final ZooKeeper zooKeeper;

    public RegistroDeServicos (ZooKeeper zooKeeper){
        this.zooKeeper = zooKeeper;
        //aqui existe uma condição de corrida
        //por conta das chamadas a exists e create
        //se dois processos tentarem criar o mesmo ZNode
        //uma das operações causará uma exceção
        //e estamos protegidos apenas ignorando ela
        try{
            if (this.zooKeeper.exists(REGISTRO_ZNODE, false) == null){
                this.zooKeeper.create(
                        REGISTRO_ZNODE,
                        new byte[]{},
                        ZooDefs.Ids.OPEN_ACL_UNSAFE,
                        CreateMode.PERSISTENT
                );
            }
        }
        catch (InterruptedException | KeeperException e){
            e.printStackTrace();
        }
    }

    public void registroJuntoAoCluster (String configuracoes) throws
            InterruptedException, KeeperException {
        if (this.zNodeAtual != null)
            System.out.printf ("Processo [%s] já registrado\n", this.zNodeAtual);
        else{
            this.zNodeAtual = this.zooKeeper.create(
                    String.format("%s/p_", REGISTRO_ZNODE),
                    configuracoes.getBytes(),
                    ZooDefs.Ids.OPEN_ACL_UNSAFE,
                    //efêmero já que um processo pode se tornar inoperante
                    //sequêncial apenas para não termos problemas com
                    //nomes repetidos
                    CreateMode.EPHEMERAL_SEQUENTIAL
            );
            System.out.printf ("Registro realizado: %s", this.zNodeAtual);
        }
    }


    public void atualizarEnderecos () throws InterruptedException, KeeperException{
        //synchronized pois poderá ser chamado por múltiplas threads
        synchronized (this){
            //watcher será criado a seguir
            List<String> znodes = zooKeeper.getChildren(REGISTRO_ZNODE, registroDeServicosWatcher);
            List <String> enderecos = new ArrayList<>(znodes.size());
            for (String znode : znodes){
                Stat stat = null;
                //depois do geChildren, verificamos se o ZNode
                //ainda existe, pois ele pode ter sido
                //destruído nesse meio tempo
                if ((stat = zooKeeper.exists(
                        String.format("%s/%s", REGISTRO_ZNODE, znode),
                        false
                )) != null){
                    enderecos.add(new String (zooKeeper.getData(
                            String.format("%s/%s", REGISTRO_ZNODE, znode),
                            false,
                            stat
                    )));
                }
            }
            this.enderecos = Collections.unmodifiableList(enderecos);
            System.out.printf ("Endereços atualmente disponíveis: %s\n", this.enderecos.toString());
        }
    }

    private Watcher registroDeServicosWatcher = (event) -> {
        try{
            atualizarEnderecos();
        }
        catch (InterruptedException | KeeperException e){
            e.printStackTrace();
        }
    };


    public void registrarseParaReceberAtualizacoes (){
        try {
            atualizarEnderecos();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    public List <String> obterTodosOsEnderecos () throws InterruptedException, KeeperException{
        synchronized (this){
            if (this.enderecos == null)
                atualizarEnderecos();
            return this.enderecos;
        }
    }

    public void removerdoCluster () throws InterruptedException, KeeperException{
        //se o ZNode realmente existe
        if (zNodeAtual != null && zooKeeper.exists(zNodeAtual, false) != null){
            //documentação: se a versão especificada é -1
            //remove qualquer versão
            zooKeeper.delete(zNodeAtual, -1);
        }
    }



}
