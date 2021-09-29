import org.apache.zookeeper.KeeperException;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class EleicaoCallbackImpl implements EleicaoCallback{
    private RegistroDeServicos registroDeServicos;
    private int porta;

    public EleicaoCallbackImpl(RegistroDeServicos registroDeServicos, int porta) {
        this.registroDeServicos = registroDeServicos;
        this.porta = porta;
    }
    @Override
    public void onEleitoLider() {
        try {
            registroDeServicos.removerdoCluster();
            registroDeServicos.registrarseParaReceberAtualizacoes();
        } catch (InterruptedException | KeeperException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onIndicadoATrabalhador() {
        try{
            String host = InetAddress.getLocalHost().getCanonicalHostName();
            String endereco = String.format(
                    "http://%s:%d",
                    host,
                    porta
            );
            registroDeServicos.registroJuntoAoCluster(endereco);

        }
        catch (InterruptedException | KeeperException | UnknownHostException e){
            e.printStackTrace();
        }
    }
}
