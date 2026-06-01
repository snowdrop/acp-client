package io.quarkiverse.acp;

import io.quarkus.runtime.QuarkusApplication;
import io.quarkus.runtime.annotations.QuarkusMain;
import org.aesh.AeshRuntimeRunner;

@QuarkusMain
public class AcpMain implements QuarkusApplication {

    @Override
    public int run(String... args) throws Exception {
        AeshRuntimeRunner.builder()
                .command(AcpCommand.class)
                .args(args)
                .execute();
        return 0;
    }
}
