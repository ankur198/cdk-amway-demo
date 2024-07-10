package com.myorg;

import java.util.HashMap;
import java.util.List;

import software.amazon.awscdk.RemovalPolicy;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;
import software.amazon.awscdk.services.lambda.FunctionUrlAuthType;
import software.amazon.awscdk.services.lambda.FunctionUrlCorsOptions;
import software.amazon.awscdk.services.lambda.FunctionUrlOptions;
import software.amazon.awscdk.services.lambda.HttpMethod;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.lambda.nodejs.NodejsFunction;
import software.amazon.awscdk.services.s3.BlockPublicAccess;
import software.amazon.awscdk.services.s3.Bucket;
import software.amazon.awscdk.services.s3.deployment.BucketDeployment;
import software.amazon.awscdk.services.s3.deployment.Source;
import software.constructs.Construct;

public class WebsiteStack extends Stack {
    public WebsiteStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        final var s3Bucket = Bucket.Builder.create(this, "LambdaS3DemoBucket")
                .removalPolicy(RemovalPolicy.DESTROY)
                .bucketName("ankurnigam-lambda-s3-demo-bucket")
                .versioned(true)
                .websiteIndexDocument("index.html")
                .blockPublicAccess(BlockPublicAccess.BLOCK_ACLS)
                .publicReadAccess(true)
                .autoDeleteObjects(true)
                .build();

        BucketDeployment.Builder.create(this, "deployFiles")
                .sources(List.of(Source.asset("./src/main/resource/s3")))
                .destinationBucket(s3Bucket)
                .build();

        final var lambda = NodejsFunction.Builder.create(this, "LambdaS3DemoFunction")
                .entry("./src/main/resource/lambda/src/index.ts")
                .handler("handler")
                .runtime(Runtime.NODEJS_20_X)
                .depsLockFilePath("./src/main/resource/lambda/package-lock.json")
                .environment(new HashMap<String, String>() {
                    {
                        put("BUCKET_NAME", s3Bucket.getBucketName());
                    }
                })
                .build();

        lambda.addFunctionUrl(FunctionUrlOptions.builder()
                .authType(FunctionUrlAuthType.NONE)
                .cors(FunctionUrlCorsOptions.builder()
                        .allowedMethods(List.of(HttpMethod.ALL))
                        .allowedHeaders(List.of("*"))
                        .allowedOrigins(List.of(s3Bucket.getBucketWebsiteUrl())).build())
                .build());

        s3Bucket.grantReadWrite(lambda);
    }
}
