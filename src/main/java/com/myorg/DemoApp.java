package com.myorg;

import software.amazon.awscdk.App;
import software.amazon.awscdk.Environment;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.Tags;

import java.util.Arrays;

public class DemoApp {
    public static void main(final String[] args) {
        App app = new App();

        var stack1 = new WebsiteStack(app, "WebsiteStack", StackProps.builder().build());
        Tags.of(stack1).add("createdBy", "ankur");

        new DevToolsStack(app, "DevToolsStack", StackProps.builder().build());

        app.synth();
    }
}

