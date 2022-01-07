package com.sergeicodes.infra;

import io.github.cdimascio.dotenv.Dotenv;
import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;

public class InfraApp {

    private final static Dotenv dotenv = Dotenv.load();

    public static void main(String[] args) {
        App app = new App();

        var props = StackProps.builder()
                .env(Environment.builder()
                        .account(dotenv.get("AWS_ACCOUNT_ID"))
                        .region(dotenv.get("AWS_REGION"))
                        .build())
                .build();

        new SergeiCodesStack(app, "SergeicodesStack", props);

        app.synth();
    }
}
