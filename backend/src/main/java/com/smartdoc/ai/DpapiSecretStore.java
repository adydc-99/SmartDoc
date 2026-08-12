package com.smartdoc.ai;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;

public final class DpapiSecretStore implements SecretStore {
    private static final int MAX_OUTPUT = 64 * 1024;
    private static final String PROTECT = "$p=[Console]::In.ReadToEnd();$b=[Text.Encoding]::UTF8.GetBytes($p);[Convert]::ToBase64String([Security.Cryptography.ProtectedData]::Protect($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))";
    private static final String UNPROTECT = "$p=[Console]::In.ReadToEnd();$b=[Convert]::FromBase64String($p);[Text.Encoding]::UTF8.GetString([Security.Cryptography.ProtectedData]::Unprotect($b,$null,[Security.Cryptography.DataProtectionScope]::CurrentUser))";
    private final Path file; private final CommandRunner runner; private volatile boolean available;
    public DpapiSecretStore(Path file){this(file,new ProcessCommandRunner(Duration.ofSeconds(5)),System.getProperty("os.name","").toLowerCase(Locale.ROOT).contains("win"));}
    DpapiSecretStore(Path file,CommandRunner runner,boolean windows){this.file=file;this.runner=runner;this.available=windows&&probe();}
    private boolean probe(){try{return "available".equals(runner.run(command("'available'"),"").trim());}catch(Exception e){return false;}}
    public boolean isAvailable(){return available;}
    public Optional<String> load(){if(!available||!Files.isRegularFile(file))return Optional.empty();try{String protectedData=Base64.getEncoder().encodeToString(Files.readAllBytes(file));return Optional.of(runner.run(command(UNPROTECT),protectedData));}catch(Exception e){available=false;return Optional.empty();}}
    public void save(String secret){if(!available)throw new SecretPersistenceException("Secure key persistence is unavailable");Path temp=null;try{String encoded=runner.run(command(PROTECT),secret).trim();byte[] protectedBytes=Base64.getDecoder().decode(encoded);Files.createDirectories(file.toAbsolutePath().getParent());temp=Files.createTempFile(file.toAbsolutePath().getParent(),"ai-key-",".tmp");Files.write(temp,protectedBytes,StandardOpenOption.TRUNCATE_EXISTING);try{Files.move(temp,file,StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);}catch(AtomicMoveNotSupportedException e){Files.move(temp,file,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception e){available=false;if(temp!=null)try{Files.deleteIfExists(temp);}catch(IOException ignored){}throw new SecretPersistenceException("Secure key persistence failed",e);}}
    public void clear(){try{Files.deleteIfExists(file);}catch(IOException e){throw new SecretPersistenceException("Could not remove persisted key",e);}}
    private static List<String> command(String script){return List.of("powershell.exe","-NoLogo","-NoProfile","-NonInteractive","-Command",script);}
    interface CommandRunner { String run(List<String> args,String stdin); }
    static final class ProcessCommandRunner implements CommandRunner {
        private final Duration timeout; ProcessCommandRunner(Duration timeout){this.timeout=timeout;}
        public String run(List<String> args,String stdin){Process process=null;try{process=new ProcessBuilder(args).redirectErrorStream(true).start();try(OutputStream out=process.getOutputStream()){out.write(stdin.getBytes(StandardCharsets.UTF_8));}Process p=process;ExecutorService pool=Executors.newSingleThreadExecutor();Future<byte[]> output=pool.submit(()->readBounded(p.getInputStream()));try{if(!process.waitFor(timeout.toMillis(),TimeUnit.MILLISECONDS)){process.destroyForcibly();throw new SecretPersistenceException("DPAPI process timed out");}byte[] bytes=output.get(1,TimeUnit.SECONDS);if(process.exitValue()!=0)throw new SecretPersistenceException("DPAPI process failed");return new String(bytes,StandardCharsets.UTF_8).trim();}finally{pool.shutdownNow();}}catch(SecretPersistenceException e){throw e;}catch(Exception e){throw new SecretPersistenceException("DPAPI process failed",e);}finally{if(process!=null&&process.isAlive())process.destroyForcibly();}}
        private static byte[] readBounded(InputStream in)throws IOException{ByteArrayOutputStream out=new ByteArrayOutputStream();byte[] b=new byte[1024];for(int n;(n=in.read(b))!=-1;){if(out.size()+n>MAX_OUTPUT)throw new IOException("DPAPI output exceeded limit");out.write(b,0,n);}return out.toByteArray();}
    }
}
