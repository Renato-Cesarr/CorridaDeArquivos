import com.jcraft.jsch.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

class Participante {
    String ip, usuario, senha;

    public Participante(String ip, String usuario, String senha) {
        this.ip = ip;
        this.usuario = usuario;
        this.senha = senha;
    }
}

class GerenciadorDeArquivos {
    private AtomicInteger contador = new AtomicInteger(0);

    public void enviarArquivo(String ip, String usuario, String senha, String arquivoOrigem, String pastaDestino) {
        int tentativas = 3;
        while (tentativas > 0) {
            try {
                JSch jsch = new JSch();
                Session sessao = jsch.getSession(usuario, ip, 22);
                sessao.setPassword(senha);
                sessao.setConfig("StrictHostKeyChecking", "no");
                sessao.setTimeout(30000);
                sessao.connect();

                ChannelSftp canalSFTP = (ChannelSftp) sessao.openChannel("sftp");
                canalSFTP.connect();

                String nomeArquivoUnico = gerarNomeArquivoUnico("renato.txt");

                String pastaAleatoria = pastaDestino + "/" + gerarSubPastaAleatoria();
                canalSFTP.mkdir(pastaAleatoria);
                canalSFTP.put(new FileInputStream(arquivoOrigem), pastaAleatoria + "/" + nomeArquivoUnico);

                canalSFTP.chmod(444, pastaAleatoria + "/" + nomeArquivoUnico);

                System.out.println("Arquivo enviado para " + ip + " como " + nomeArquivoUnico);

                canalSFTP.disconnect();
                sessao.disconnect();
                break;
            } catch (Exception e) {
                System.err.println("Erro ao enviar arquivo para " + ip + ": " + e.getMessage());
                tentativas--;
                if (tentativas > 0) {
                    try {
                        System.out.println("Tentando novamente em 5 segundos...");
                        Thread.sleep(500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                } else {
                    System.err.println("Falha ao enviar arquivo após várias tentativas.");
                }
            }
        }
    }

    private String gerarNomeArquivoUnico(String nomeArquivoOriginal) {
        int idUnico = contador.incrementAndGet();
        return nomeArquivoOriginal + "_" + System.nanoTime() + "_" + idUnico;
    }

    private String gerarSubPastaAleatoria() {
        return "pastaRenatu_" + UUID.randomUUID().toString();
    }
}

public class HighSpeedFileCorrida {
    public static void main(String[] args) {
        String arquivoOrigem = "/home/almaviva-linux/localArquivo/renato.txt";
        List<Participante> participantes = Arrays.asList(
            new Participante("192.168.208.42", "thales-almaviva", "thales@2203"),
            new Participante("192.168.208.73", "almaviva-linux", "magna@123"),
            new Participante("192.168.208.90", "almaviva-linux", "C4liforni4"),
            new Participante("192.168.208.53", "almaviva", "magna@123"),
            new Participante("192.168.208.46", "almaviva-linux", "Almaviva04092004"),
            new Participante("192.168.208.49", "matheus", "magna@123"),
            new Participante("192.168.208.101", "almaviva-linux", "Magna@123"),
            new Participante("192.168.208.95", "vitor", "magna@123")
        );

        String modeloPastaDestino = "/home/{usuario}/GuerraArquivos";

        ExecutorService executor = Executors.newFixedThreadPool(30);

        executor.submit(() -> iniciarAtacante(participantes, arquivoOrigem, modeloPastaDestino, executor));
        executor.submit(() -> iniciarDefesa("/home/almaviva-linux/GuerraArquivos"));

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Encerrando o programa...");
            executor.shutdownNow();
        }));
    }

    private static void iniciarAtacante(List<Participante> participantes, String arquivoOrigem, String modeloPastaDestino,
                                        ExecutorService executor) {
        GerenciadorDeArquivos gerenciadorDeArquivos = new GerenciadorDeArquivos();

        while (true) {
            for (Participante participante : participantes) {
                String pastaDestino = modeloPastaDestino.replace("{usuario}", participante.usuario);

                executor.submit(() -> gerenciadorDeArquivos.enviarArquivo(participante.ip, participante.usuario, participante.senha,
                        arquivoOrigem, pastaDestino));

                try {
                    Thread.sleep(10);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private static void iniciarDefesa(String caminhoPastaAlvo) {
        File pastaAlvo = new File(caminhoPastaAlvo);

        while (true) {
            try {
                excluirConteudoPasta(pastaAlvo);
                System.out.println("Todos os conteúdos apagados da pasta: " + caminhoPastaAlvo);

                Thread.sleep(50);
            } catch (Exception e) {
                System.err.println("Erro ao executar a defesa: " + e.getMessage());
            }
        }
    }

    private static void excluirConteudoPasta(File pasta) throws IOException {
        if (!pasta.exists()) {
            return;
        }

        for (File arquivo : pasta.listFiles()) {
            if (arquivo.isDirectory()) {
                excluirConteudoPasta(arquivo);
            }
            try {
                Set<PosixFilePermission> permissao = PosixFilePermissions.fromString("rwxrwxrwx");
                Files.setPosixFilePermissions(arquivo.toPath(), permissao);
            } catch (UnsupportedOperationException | IOException e) {
                System.err.println("Erro ao alterar permissões: " + e.getMessage());
            }
            arquivo.delete();
        }
    }
}
